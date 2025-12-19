package com.github.maharong.smartpos.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 점검 추천 배치 응답 DTO.
 *
 * @param batchId 배치 ID
 * @param productId 상품 ID
 * @param productName 상품명
 * @param expiryDate 유통기한
 * @param quantity 현재(예상) 수량
 * @param lastCheckedAt 마지막 점검 일시(없으면 null)
 * @param score 우선순위 점수(내림차순 정렬용)
 * @param reasons 추천 사유 목록
 */
public record InventoryAuditRecommendationResponse(
        Long batchId,
        Long productId,
        String productName,
        LocalDate expiryDate,
        int quantity,
        LocalDateTime lastCheckedAt,
        int score,
        List<String> reasons
) {
}