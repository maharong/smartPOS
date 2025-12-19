package com.github.maharong.smartpos.dto;

import com.github.maharong.smartpos.enums.PaymentMethod;

import java.util.List;

/**
 * 판매 생성 요청 DTO.
 *
 * @param paymentMethod 결제 수단
 * @param cashAmount 현금 결제 금액
 * @param cardAmount 카드 결제 금액
 * @param pointAmount 포인트 사용 금액
 * @param lines 판매 라인 목록
 */
public record SaleCreateRequest(
        PaymentMethod paymentMethod,
        long cashAmount,
        long cardAmount,
        long pointAmount,
        List<SaleCreateLineRequest> lines
) {
}