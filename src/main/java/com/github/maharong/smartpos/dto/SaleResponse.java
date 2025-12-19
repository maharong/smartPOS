package com.github.maharong.smartpos.dto;

import com.github.maharong.smartpos.entity.Sale;
import com.github.maharong.smartpos.entity.SaleItem;
import com.github.maharong.smartpos.enums.PaymentMethod;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 판매(영수증) 응답 DTO.
 *
 * @param saleId 판매 ID
 * @param soldAt 판매 시각
 * @param status 판매 상태
 * @param paymentMethod 결제 수단
 * @param totalPrice 총액
 * @param cashAmount 현금 결제 금액
 * @param cardAmount 카드 결제 금액
 * @param pointAmount 포인트 사용 금액
 * @param lines 판매 라인 목록
 */
public record SaleResponse(
        Long saleId,
        LocalDateTime soldAt,
        String status,
        PaymentMethod paymentMethod,
        long totalPrice,
        long cashAmount,
        long cardAmount,
        long pointAmount,
        List<SaleLineResponse> lines
) {
    /**
     * {@link Sale} + {@link SaleItem} 목록을 응답 DTO로 변환한다.
     *
     * @param sale 판매 엔티티
     * @param items 판매 라인 목록
     * @return 판매 응답 DTO
     */
    public static SaleResponse from(Sale sale, List<SaleItem> items) {
        return new SaleResponse(
                sale.getId(),
                sale.getSoldAt(),
                sale.getStatus().name(),
                sale.getPaymentMethod(),
                sale.getTotalPrice(),
                sale.getCashAmount(),
                sale.getCardAmount(),
                sale.getPointAmount(),
                items.stream().map(SaleLineResponse::from).toList()
        );
    }
}
