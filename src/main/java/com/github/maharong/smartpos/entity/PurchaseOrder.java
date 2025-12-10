package com.github.maharong.smartpos.entity;

import com.github.maharong.smartpos.enums.PurchaseOrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 발주 1건(발주서 한 장)을 나타내는 엔티티.
 * 언제 발주했는지, 현재 상태, 총 발주 금액 등의 공통 정보를 담는다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id; // 발주 고유 ID

    @Column(nullable = false)
    private LocalDateTime orderedAt; // 발주 생성 시각

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PurchaseOrderStatus status; // 발주 상태 (발주 대기, 주문 접수, 취소 등)

    @Column(nullable = false)
    private long totalPrice; // 이 발주의 총 금액

    @Builder
    public PurchaseOrder(LocalDateTime orderedAt, PurchaseOrderStatus status, long totalPrice) {
        this.orderedAt = orderedAt;
        this.status = status;
        this.totalPrice = totalPrice;
    }

    public void updateStatus(PurchaseOrderStatus newStatus) {
        this.status = newStatus;
    }

    public void updateTotalPrice(long totalPrice) {
        this.totalPrice = totalPrice;
    }
}
