package com.github.maharong.smartpos.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 재고 현황을 담당하는 엔티티.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class InventoryBatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 배치 고유 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product; // 상품 정보

    @Column(nullable = false)
    private int quantity; // 남은 수량

    @Column(nullable = false)
    private LocalDate expiryDate; // 유통기한

    @Column(nullable = false)
    private LocalDate receivedDate; // 입고일

    @Builder
    public InventoryBatch(Product product, int quantity, LocalDate expiryDate, LocalDate receivedDate) {
        this.product = product;
        this.quantity = quantity;
        this.expiryDate = expiryDate;
        this.receivedDate = receivedDate;
    }
}
