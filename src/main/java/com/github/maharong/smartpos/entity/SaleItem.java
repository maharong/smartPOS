package com.github.maharong.smartpos.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 개별 판매 라인(판매된 상품 1종류)을 나타내는 엔티티.
 * 어느 판매(Sale)에서 어떤 상품(Product)이 몇 개, 얼마에 팔렸는지를 기록한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class SaleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 판매 라인 고유 ID

    /**
     * 어느 판매(Sale)에 속한 라인인지.
     * 영수증 1장(Sale)에 여러 SaleItem이 매달리는 구조.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    /**
     * 어떤 상품이 팔렸는지 상품 정보.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity; // 판매 수량 (낱개 기준)

    /**
     * 판매 당시 단가(원 단위).
     * 나중에 가격이 바뀌어도 과거 판매 기록에는 영향을 주지 않도록 저장.
     */
    @Column(nullable = false)
    private int unitPrice;

    @Builder
    public SaleItem(Sale sale, Product product, int quantity, int unitPrice) {
        this.sale = sale;
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    /**
     * 이 라인의 총 금액을 계산하는 메서드.
     */
    public int getLineTotal() {
        return unitPrice * quantity;
    }
}