package com.github.maharong.smartpos.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 발주 내 개별 상품 라인.
 * 어떤 발주서(PurchaseOrder)에서 어떤 상품(Product)을
 * 몇 박스 주문했는지와 박스 단가를 기록한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class PurchaseOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id; // 발주 라인 고유 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder; // 어떤 발주에 속해있는지

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product; // 어느 상품인지

    /**
     * 발주 단위(박스) 기준 수량.
     * 예: packageQuantity = 3 → 3박스 발주
     */
    @Column(nullable = false)
    private int packageQuantity;

    /**
     * 발주 당시 박스(패키지) 단가.
     * 예: 1박스(24개) = 14,400원
     */
    @Column(nullable = false)
    private int unitPrice;

    @Builder
    public PurchaseOrderItem(PurchaseOrder purchaseOrder, Product product, int packageQuantity, int unitPrice) {
        this.purchaseOrder = purchaseOrder;
        this.product = product;
        this.packageQuantity = packageQuantity;
        this.unitPrice = unitPrice;
    }

    /**
     * 이 라인의 총 금액을 계산한다.
     * (박스 수량 × 박스 단가)
     */
    public long getLineTotal() {
        return (long) packageQuantity * unitPrice;
    }
}