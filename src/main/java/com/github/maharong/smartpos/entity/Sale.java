package com.github.maharong.smartpos.entity;

import com.github.maharong.smartpos.enums.PaymentMethod;
import com.github.maharong.smartpos.enums.SaleStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 판매 1건(영수증 1장)을 나타내는 엔티티.
 * 결제 시간, 결제 상태, 결제 수단 등의 공통 정보를 담는다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 판매 고유 ID (영수증 번호 느낌)

    @Column(nullable = false)
    private LocalDateTime soldAt; // 판매(결제) 시각

    @Column(nullable = false)
    private long totalPrice; // 총 결제 금액 (원 단위)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod paymentMethod; // 결제 수단 (현금, 카드, 포인트, 혼합 등)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SaleStatus status; // 판매 상태 (결제 완료, 취소, 환불 등)

    @Column(nullable = false)
    private long cashAmount; // 현금 결제 금액

    @Column(nullable = false)
    private long cardAmount; // 카드 결제 금액

    @Column(nullable = false)
    private long pointAmount; // 포인트 사용 금액

    @Builder
    public Sale(LocalDateTime soldAt, long totalPrice, PaymentMethod paymentMethod, SaleStatus status) {
        this.soldAt = soldAt;
        this.totalPrice = totalPrice;
        this.paymentMethod = paymentMethod;
        this.status = status;
    }

    /**
     * 판매 상태 변경 (취소/환불 등)
     */
    public void changeStatus(SaleStatus newStatus) {
        this.status = newStatus;
    }

    /**
     * 총 결제 금액을 변경한다.
     *
     * <p>판매 생성 시 판매 라인(SaleItem) 기준으로 계산된 총액을 반영할 때 사용한다.</p>
     *
     * @param totalPrice 총 결제 금액(0 이상)
     * @throws IllegalArgumentException {@code totalPrice}가 0 미만인 경우
     */
    public void changeTotalPrice(long totalPrice) {
        if (totalPrice < 0) {
            throw new IllegalArgumentException("총액은 0 이상이어야 합니다.");
        }
        this.totalPrice = totalPrice;
    }

    /**
     * 결제 수단별 결제 금액을 설정한다.
     *
     * @param cashAmount 현금 결제 금액(0 이상)
     * @param cardAmount 카드 결제 금액(0 이상)
     * @param pointAmount 포인트 사용 금액(0 이상)
     * @throws IllegalArgumentException 음수인 금액이 있는 경우
     */
    public void changePaymentAmounts(long cashAmount, long cardAmount, long pointAmount) {
        if (cashAmount < 0 || cardAmount < 0 || pointAmount < 0) {
            throw new IllegalArgumentException("결제 금액은 음수일 수 없습니다.");
        }
        this.cashAmount = cashAmount;
        this.cardAmount = cardAmount;
        this.pointAmount = pointAmount;
    }
}