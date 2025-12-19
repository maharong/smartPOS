package com.github.maharong.smartpos.dto;

import com.github.maharong.smartpos.entity.SaleItem;

/**
 * 판매 라인 응답 DTO.
 *
 * @param productId 상품 ID
 * @param productName 상품명
 * @param quantity 수량
 * @param unitPrice 단가
 * @param lineTotal 라인 합계(단가 * 수량)
 */
public record SaleLineResponse(
        Long productId,
        String productName,
        int quantity,
        int unitPrice,
        long lineTotal
) {
    /**
     * {@link SaleItem}을 응답 DTO로 변환한다.
     *
     * @param item 판매 라인 엔티티
     * @return 판매 라인 응답 DTO
     */
    public static SaleLineResponse from(SaleItem item) {
        long total = (long) item.getUnitPrice() * item.getQuantity();
        return new SaleLineResponse(
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                total
        );
    }
}