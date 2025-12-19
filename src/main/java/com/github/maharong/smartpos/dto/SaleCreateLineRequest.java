package com.github.maharong.smartpos.dto;

/**
 * 판매 생성 요청 라인 DTO.
 *
 * @param productId 상품 ID
 * @param quantity 수량
 */
public record SaleCreateLineRequest(
        Long productId,
        int quantity
) {}