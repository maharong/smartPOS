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
}