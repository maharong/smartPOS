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
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 재고 관련 입출고/폐기/조회 기능을 제공하는 서비스.
 *
 * <p>판매 출고는 유통기한 기준 선출(FEFO: First-Expire, First-Out)로 배치를 차감한다.
 * 또한 기준일({@code baseDate}) 이전에 만료된 배치는 판매에 사용하지 않는다.</p>
 */
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
     * 판매 출고를 처리한다. (기본 기준일: 오늘)
     *
     * <p>유통기한이 빠른 배치부터(FEFO) 재고를 차감하며, 이미 만료된 배치는 제외한다.</p>
     *
     * @param product 판매 상품
     * @param quantity 판매 수량 (1 이상)
     * @throws IllegalArgumentException {@code quantity}가 1 미만인 경우
     * @throws IllegalStateException 재고가 부족하여 요청 수량을 모두 차감할 수 없는 경우
     */
    public void consumeForSale(Product product, int quantity) {
        consumeForSale(product, quantity, LocalDate.now());
    }

    /**
     * 판매 출고를 처리한다. (기준일 지정)
     *
     * <p>기준일 {@code baseDate}를 기준으로 만료 배치를 제외하고,
     * 유통기한이 빠른 배치부터(FEFO) 재고를 차감한다.</p>
     *
     * <ul>
     *   <li>만료 배치 제외 조건: {@code expiryDate.isBefore(baseDate)}</li>
     *   <li>차감 순서: {@code expiryDate} 오름차순</li>
     * </ul>
     *
     * @param product 판매 상품
     * @param quantity 판매 수량 (1 이상)
     * @param baseDate 만료 여부 판단 기준일 (예: 판매일)
     * @throws IllegalArgumentException {@code quantity}가 1 미만인 경우
     * @throws IllegalStateException 재고가 부족하여 요청 수량을 모두 차감할 수 없는 경우
     */
    public void consumeForSale(Product product, int quantity, LocalDate baseDate) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");
        }

        int remaining = quantity;

        // FEFO를 위해 expiryDate 오름차순으로 배치를 조회한다.
        List<InventoryBatch> batches =
                inventoryBatchRepository.findByProductOrderByExpiryDateAsc(product);

        for (InventoryBatch batch : batches) {
            if (remaining == 0) break;
            if (batch.getQuantity() <= 0) continue;

            // 만료 배치는 판매에 사용하지 않는다.
            if (batch.getExpiryDate().isBefore(baseDate)) continue;

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
     * 재고 현황(전체 상품 요약 리스트)을 조회한다.
     * <p>
     * 기본적으로 {@link ProductStatus#ACTIVE} 상품만 재고 현황에 노출하는 것을 권장한다.
     * (발주 중단/단종 상품은 현황 화면에서 제외하기 위함)
     * </p>
     *
     * @param status 조회할 상품 상태(기본: ACTIVE)
     * @return 전체 상품 재고 요약 목록
     */
    @Transactional(readOnly = true)
    public List<InventorySummaryResponse> getAllSummaries(ProductStatus status) {
        ProductStatus targetStatus = (status == null) ? ProductStatus.ACTIVE : status;
        LocalDate today = LocalDate.now();

        return inventoryBatchRepository.findAllInventorySummaries(today, targetStatus).stream()
                .map(row -> new InventorySummaryResponse(
                        row.getProductId(),
                        row.getProductName(),
                        Math.toIntExact(row.getTotalQuantity())
                ))
                .toList();
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

    /**
     * 점검 추천 배치 목록을 생성하여 반환한다.
     *
     * <p>추천 조건은 다음 3가지이며, 조건에 해당하는 배치만 결과에 포함한다.</p>
     * <ul>
     *   <li>유통기한 만료: {@code expiryDate < baseDate}</li>
     *   <li>유통기한 임박: 만료가 아니면서 {@code daysUntilExpiry <= expiringDays}</li>
     *   <li>오래 미점검:
     *     <ul>
     *       <li>점검 기록 없음: {@code lastCheckedAt == null}</li>
     *       <li>마지막 점검일이 오래됨: {@code lastCheckedAt.toLocalDate()} 기준 {@code staleDays} 이상 경과</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * <p>후보 배치는 리포지토리에서 2번 조회하여 합집합으로 병합한다.</p>
     * <ul>
     *   <li>유통기한 기준 후보: {@code expiryDate <= baseDate + expiringDays}</li>
     *   <li>미점검 기준 후보: {@code lastCheckedAt is null OR lastCheckedAt <= now - staleDays}</li>
     * </ul>
     *
     * <p>결과는 점수({@code score}) 내림차순, 동일 점수일 경우 유통기한({@code expiryDate}) 오름차순으로 정렬한다.</p>
     *
     * @param baseDate 기준일(만료/임박 및 미점검 기간 계산의 기준)
     * @param expiringDays 임박으로 판단할 남은 일수
     * @param staleDays 오래 미점검으로 판단할 경과 일수
     * @param limit 최대 반환 개수(0 이하이면 빈 리스트 반환)
     * @return 점검 추천 배치 목록
     */
    @Transactional(readOnly = true)
    public List<InventoryAuditRecommendationResponse> getAuditRecommendations(
            LocalDate baseDate,
            int expiringDays,
            int staleDays,
            int limit
    ) {
        LocalDate expiryCutoff = baseDate.plusDays(expiringDays);
        LocalDateTime staleCutoff = LocalDateTime.now().minusDays(staleDays);

        List<InventoryBatch> byExpiry = inventoryBatchRepository.findAuditCandidatesByExpiry(expiryCutoff);
        List<InventoryBatch> byStale = inventoryBatchRepository.findAuditCandidatesByStaleCheck(staleCutoff);

        Map<Long, InventoryBatch> merged = new HashMap<>();
        for (InventoryBatch b : byExpiry) merged.put(b.getId(), b);
        for (InventoryBatch b : byStale) merged.put(b.getId(), b);

        List<InventoryAuditRecommendationResponse> results = new ArrayList<>();

        for (InventoryBatch b : merged.values()) {
            boolean expired = b.getExpiryDate().isBefore(baseDate);
            long daysUntilExpiry = ChronoUnit.DAYS.between(baseDate, b.getExpiryDate());
            boolean expiringSoon = !expired && daysUntilExpiry <= expiringDays;

            boolean neverChecked = (b.getLastCheckedAt() == null);
            boolean staleChecked = !neverChecked
                    && ChronoUnit.DAYS.between(b.getLastCheckedAt().toLocalDate(), baseDate) >= staleDays;

            List<String> reasons = new ArrayList<>();
            int score = 0;

            // 1) 만료 / 2) 임박
            if (expired) {
                reasons.add("EXPIRED");
                score += 100;
            } else if (expiringSoon) {
                reasons.add("EXPIRING_SOON");
                score += Math.max(0, 50 - (int) daysUntilExpiry);
            }

            // 3) 오래 미점검
            if (neverChecked) {
                reasons.add("NEVER_CHECKED");
                score += 40;
            } else if (staleChecked) {
                reasons.add("STALE_CHECK");
                score += 20;
            }

            // 어떤 조건에도 해당하지 않으면 결과에서 제외한다.
            if (reasons.isEmpty()) continue;

            results.add(new InventoryAuditRecommendationResponse(
                    b.getId(),
                    b.getProduct().getId(),
                    b.getProduct().getName(),
                    b.getExpiryDate(),
                    b.getQuantity(),
                    b.getLastCheckedAt(),
                    score,
                    reasons
            ));
        }

        results.sort(
                Comparator.comparingInt(InventoryAuditRecommendationResponse::score)
                        .reversed()
                        .thenComparing(InventoryAuditRecommendationResponse::expiryDate)
        );

        if (limit <= 0) return List.of();
        return results.size() > limit ? results.subList(0, limit) : results;
    }

    /**
     * 특정 배치를 점검 완료 처리한다.
     *
     * <p>배치의 {@code lastCheckedAt}을 현재 시각으로 갱신한다.</p>
     *
     * @param batchId 점검 처리할 배치 ID
     * @throws IllegalArgumentException 배치를 찾을 수 없는 경우
     */
    public void checkBatch(Long batchId) {
        InventoryBatch batch = inventoryBatchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("배치를 찾을 수 없습니다. id=" + batchId));
        batch.markChecked(LocalDateTime.now());
    }
}