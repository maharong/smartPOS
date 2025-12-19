package com.github.maharong.smartpos.repository;

import com.github.maharong.smartpos.entity.InventoryBatch;
import com.github.maharong.smartpos.entity.Product;
import com.github.maharong.smartpos.enums.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface InventoryBatchRepository extends JpaRepository<InventoryBatch, Long> {
    // FEFO(유통기한 빠른 순) 출고를 위한 배치 조회
    List<InventoryBatch> findByProductOrderByExpiryDateAsc(Product product);

    // 유통기한 임박 배치 조회 (<= date)
    List<InventoryBatch> findByExpiryDateLessThanEqual(LocalDate date);

    // 유통기한이 기준일보다 "이전"(expired) 이고, 수량이 남아있는 배치
    List<InventoryBatch> findByExpiryDateBeforeAndQuantityGreaterThan(LocalDate baseDate, int quantity);

    /**
     * 재고 현황(전체 상품 요약)을 조회하기 위한 프로젝션.
     */
    interface InventorySummaryProjection {
        Long getProductId();

        String getProductName();

        Long getTotalQuantity();
    }

    /**
     * 전체 상품 재고 요약을 조회한다.
     * <p>
     * 판매 가능 재고 기준(수량 > 0, 만료되지 않음)을 만족하는 배치 수량만 합산한다.
     * 상품은 배치가 없어도(재고 0이어도) 조회 대상에 포함되며, 이 경우 합계는 0으로 반환된다.
     * </p>
     *
     * @param today  만료 여부 판단 기준일(보통 오늘)
     * @param status 상품 상태 필터
     * @return 상품별 재고 요약 목록
     */
    @Query("""
            select
                p.id as productId,
                p.name as productName,
                coalesce(sum(b.quantity), 0) as totalQuantity
            from Product p
            left join InventoryBatch b
                on b.product = p
                and b.quantity > 0
                and b.expiryDate >= :today
            where p.status = :status
            group by p.id, p.name
            order by p.name asc
            """)
    List<InventorySummaryProjection> findAllInventorySummaries(
            @Param("today") LocalDate today,
            @Param("status") ProductStatus status
    );

    /**
     * 유통기한 기준으로 점검 추천 후보 배치를 조회한다.
     *
     * <p>기준일({@code cutoff})까지(포함) 유통기한이 도달한 배치를 대상으로 하며,
     * 수량이 남아있는 배치({@code quantity > 0})만 반환한다.</p>
     *
     * <p>배치 목록 조회 시 N+1 조회를 방지하기 위해 {@code product}를 fetch join 한다.</p>
     *
     * @param cutoff 조회 기준 유통기한(포함)
     * @return 유통기한 오름차순으로 정렬된 점검 후보 배치 목록
     */
    @Query("""
        select b
        from InventoryBatch b
        join fetch b.product
        where b.quantity > 0
          and b.expiryDate <= :cutoff
        order by b.expiryDate asc
        """)
    List<InventoryBatch> findAuditCandidatesByExpiry(
            @Param("cutoff") LocalDate cutoff
    );

    /**
     * 마지막 점검일 기준으로 점검 추천 후보 배치를 조회한다.
     *
     * <p>다음 조건에 해당하는 배치를 대상으로 하며, 수량이 남아있는 배치({@code quantity > 0})만 반환한다.</p>
     * <ul>
     *   <li>점검 기록이 없는 배치({@code lastCheckedAt is null})</li>
     *   <li>마지막 점검일이 기준 시각({@code cutoff}) 이전(또는 동일)인 배치</li>
     * </ul>
     *
     * <p>배치 목록 조회 시 N+1 조회를 방지하기 위해 {@code product}를 fetch join 한다.</p>
     *
     * @param cutoff 오래 미점검 여부 판단 기준 시각(포함)
     * @return 점검 후보 배치 목록
     */
    @Query("""
        select b
        from InventoryBatch b
        join fetch b.product
        where b.quantity > 0
          and (b.lastCheckedAt is null or b.lastCheckedAt <= :cutoff)
        """)
    List<InventoryBatch> findAuditCandidatesByStaleCheck(
            @Param("cutoff") LocalDateTime cutoff
    );

}
