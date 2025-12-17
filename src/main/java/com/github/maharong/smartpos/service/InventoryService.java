package com.github.maharong.smartpos.service;

import com.github.maharong.smartpos.dto.*;
import com.github.maharong.smartpos.entity.InventoryBatch;
import com.github.maharong.smartpos.entity.InventoryLog;
import com.github.maharong.smartpos.entity.Product;
import com.github.maharong.smartpos.enums.InventoryConsumeType;
import com.github.maharong.smartpos.enums.ProductStatus;
import com.github.maharong.smartpos.repository.InventoryBatchRepository;
import com.github.maharong.smartpos.repository.InventoryLogRepository;
import com.github.maharong.smartpos.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryService {

    private final ProductRepository productRepository;
    private final InventoryBatchRepository inventoryBatchRepository;
    private final InventoryLogRepository inventoryLogRepository;

    /**
     * 입고 처리(배치 생성)를 수행한다.
     * <p>
     * 입고는 {@link InventoryBatch}를 생성하는 것으로 표현한다. {@code receivedDate}가 없다면
     * 오늘 날짜로 처리하며, 유통기한이 입고일보다 빠른 경우는 허용하지 않는다.
     * </p>
     *
     * <ul>
     *   <li>단종({@link ProductStatus#DISCONTINUED}) 상품은 신규 입고를 막는다.</li>
     *   <li>{@code expiryDate}가 {@code receivedDate}보다 빠르면({@code expiryDate < receivedDate}) 예외를 발생시킨다.</li>
     * </ul>
     *
     * @param req 입고 요청 DTO
     * @return 생성된 배치 응답 DTO
     * @throws IllegalArgumentException 상품이 없거나 날짜 검증에 실패한 경우
     * @throws IllegalStateException 단종 상품을 입고하려는 경우
     */
    public InventoryBatchResponse receive(InventoryReceiveRequest req) {
        Product product = productRepository.findById(req.productId())
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. id=" + req.productId()));

        if (product.getStatus() == ProductStatus.DISCONTINUED) {
            throw new IllegalStateException("단종 상품은 입고할 수 없습니다. productId=" + product.getId());
        }

        LocalDate receivedDate = (req.receivedDate() != null) ? req.receivedDate() : LocalDate.now();
        if (req.expiryDate().isBefore(receivedDate)) {
            throw new IllegalArgumentException("유통기한이 입고일보다 빠릅니다. expiry=" + req.expiryDate() + ", received=" + receivedDate);
        }

        InventoryBatch batch = InventoryBatch.builder()
                .product(product)
                .quantity(req.quantity())
                .expiryDate(req.expiryDate())
                .receivedDate(receivedDate)
                .build();

        InventoryBatch saved = inventoryBatchRepository.save(batch);
        return InventoryBatchResponse.from(saved);
    }

    /**
     * 관리자 출고 처리를 수행한다.
     * <p>
     * 출고는 FEFO(First-Expired, First-Out) 기준으로 배치 수량을 차감한다.
     * 즉, 유통기한이 더 빠른 배치부터 먼저 차감한다. 차감 내역은 배치 단위로
     * {@link InventoryLog}에 기록한다.
     * </p>
     *
     * <ul>
     *   <li>요청 수량은 여러 배치에 걸쳐 분할 차감될 수 있다.</li>
     *   <li>재고가 부족하면 예외를 발생시키며, 트랜잭션으로 인해 부분 차감은 롤백된다.</li>
     *   <li>로그 사유는 {@link InventoryConsumeType}을 사용한다.</li>
     * </ul>
     *
     * @param req 출고 요청 DTO
     * @throws IllegalArgumentException 상품이 존재하지 않는 경우
     * @throws IllegalStateException 재고가 부족한 경우
     */
    public void consume(InventoryConsumeRequest req) {
        Product product = productRepository.findById(req.productId())
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. id=" + req.productId()));

        int remaining = req.quantity();

        List<InventoryBatch> batches = inventoryBatchRepository.findByProductOrderByExpiryDateAsc(product);

        for (InventoryBatch batch : batches) {
            if (remaining == 0) break;
            if (batch.getQuantity() <= 0) continue;

            int take = Math.min(batch.getQuantity(), remaining);
            batch.decrease(take);

            InventoryLog log = InventoryLog.builder()
                    .product(product)
                    .batch(batch)
                    .type(req.type())
                    .quantity(take)
                    .note(req.note())
                    .build();

            inventoryLogRepository.save(log);

            remaining -= take;
        }

        if (remaining > 0) {
            throw new IllegalStateException("재고 부족: 요청=" + req.quantity() + ", 부족=" + remaining);
        }
    }

    /**
     * 판매 차감 처리를 수행한다(로그 미기록).
     * <p>
     * 판매 이력은 {@code Sale}/{@code SaleItem}에서 관리한다는 정책에 따라,
     * 이 메서드는 {@link InventoryLog}를 남기지 않는다.
     * FEFO 기준으로 {@link InventoryBatch}의 수량만 차감한다.
     * </p>
     *
     * @param product 판매 대상 상품 엔티티
     * @param quantity 차감할 수량(양수)
     * @throws IllegalStateException 재고가 부족한 경우
     */
    public void consumeForSale(Product product, int quantity) {
        int remaining = quantity;

        List<InventoryBatch> batches = inventoryBatchRepository.findByProductOrderByExpiryDateAsc(product);

        for (InventoryBatch batch : batches) {
            if (remaining == 0) break;
            if (batch.getQuantity() <= 0) continue;

            int take = Math.min(batch.getQuantity(), remaining);
            batch.decrease(take);
            remaining -= take;
        }

        if (remaining > 0) {
            throw new IllegalStateException("재고 부족(판매): 요청=" + quantity + ", 부족=" + remaining);
        }
    }

    /**
     * 특정 상품의 배치 목록을 조회한다.
     * <p>
     * 배치는 유통기한 오름차순으로 반환한다(출고 기준 FEFO 정렬과 동일한 방향).
     * </p>
     *
     * @param productId 상품 ID
     * @return 배치 응답 목록
     * @throws IllegalArgumentException 상품이 존재하지 않는 경우
     */
    @Transactional(readOnly = true)
    public List<InventoryBatchResponse> getBatches(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. id=" + productId));

        return inventoryBatchRepository.findByProductOrderByExpiryDateAsc(product)
                .stream()
                .map(InventoryBatchResponse::from)
                .toList();
    }

    /**
     * 특정 상품의 재고 요약을 계산한다.
     * <p>
     * '판매 가능 재고'는 다음 조건을 만족하는 배치들의 수량 합으로 정의한다.
     * </p>
     *
     * <ul>
     *   <li>배치 수량이 {@code 0}보다 크다({@code quantity > 0})</li>
     *   <li>유통기한이 오늘 날짜보다 빠르지 않다(오늘이 유통기한인 배치도 포함)</li>
     * </ul>
     *
     * @param productId 상품 ID
     * @return 재고 요약 응답 DTO
     * @throws IllegalArgumentException 상품이 존재하지 않는 경우
     */
    @Transactional(readOnly = true)
    public InventorySummaryResponse getSummary(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. id=" + productId));

        LocalDate today = LocalDate.now();

        int total = inventoryBatchRepository.findByProductOrderByExpiryDateAsc(product).stream()
                .filter(b -> b.getQuantity() > 0)
                .filter(b -> !b.getExpiryDate().isBefore(today))
                .mapToInt(InventoryBatch::getQuantity)
                .sum();

        return new InventorySummaryResponse(product.getId(), product.getName(), total);
    }

    /**
     * 유통기한 임박/만료 배치를 조회한다.
     * <p>
     * {@code baseDate}를 기준으로 유통기한이 {@code baseDate}와 같거나 더 이른 배치
     * (즉 {@code expiryDate <= baseDate})를 조회한다.
     * </p>
     *
     * <p>
     * 응답의 {@code daysToExpiry}는 오늘 날짜 기준으로 계산하며, 이미 만료된 배치는
     * 음수 값이 될 수 있다.
     * </p>
     *
     * @param baseDate 유통기한 비교 기준일(이 날짜까지 포함)
     * @return 유통기한이 가까운 순으로 정렬된 배치 목록
     */
    @Transactional(readOnly = true)
    public List<ExpiringBatchResponse> getExpiringBatches(LocalDate baseDate) {
        LocalDate today = LocalDate.now();

        return inventoryBatchRepository.findByExpiryDateLessThanEqual(baseDate).stream()
                .map(b -> new ExpiringBatchResponse(
                        b.getId(),
                        b.getProduct().getId(),
                        b.getProduct().getName(),
                        b.getQuantity(),
                        b.getExpiryDate(),
                        ChronoUnit.DAYS.between(today, b.getExpiryDate())
                ))
                .sorted(Comparator.comparing(ExpiringBatchResponse::daysToExpiry))
                .toList();
    }

    /**
     * 유통기한 만료 배치를 일괄 폐기 처리한다.
     * <p>
     * 유통기한이 {@code baseDate}보다 빠른 배치(즉 {@code expiryDate < baseDate}) 중
     * 수량이 남아있는 배치({@code quantity > 0})를 대상으로 전량 폐기 처리한다.
     * </p>
     *
     * <ul>
     *   <li>대상 배치의 수량을 {@code 0}으로 만든다.</li>
     *   <li>배치별로 {@link InventoryConsumeType#WASTE} 로그를 {@link InventoryLog}에 남긴다.</li>
     *   <li>{@code note}가 비어있으면 기본 문구를 사용한다.</li>
     * </ul>
     *
     * @param baseDate 만료 판단 기준일(이 날짜 이전이 만료로 처리됨)
     * @param note 로그에 남길 메모(비어있으면 기본 문구 사용)
     * @return 처리 결과(기준일, 처리 배치 수, 폐기 수량 합계)
     */
    @Transactional
    public DisposeExpiredResponse disposeExpiredBatches(LocalDate baseDate, String note) {
        String memo = (note == null || note.isBlank()) ? "유통기한 만료 일괄 폐기" : note;

        List<InventoryBatch> expired = inventoryBatchRepository
                .findByExpiryDateBeforeAndQuantityGreaterThan(baseDate, 0);

        int totalDisposed = 0;

        for (InventoryBatch batch : expired) {
            int disposedQty = batch.getQuantity();
            if (disposedQty <= 0) continue;

            batch.decrease(disposedQty);

            InventoryLog log = InventoryLog.builder()
                    .product(batch.getProduct())
                    .batch(batch)
                    .type(InventoryConsumeType.WASTE)
                    .quantity(disposedQty)
                    .note(memo)
                    .build();

            inventoryLogRepository.save(log);

            totalDisposed += disposedQty;
        }

        return new DisposeExpiredResponse(baseDate, expired.size(), totalDisposed);
    }
}