package com.github.maharong.smartpos.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 상품 정보를 담은 엔티티.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 상품 고유 ID

    @Column(nullable = false)
    private String name; // 상품 이름

    @Column(nullable = false)
    private int price; // 상품 가격

    @Column(nullable = false, unique = true)
    private String barcode; // 바코드

    /**
     * 하나의 발주 단위(박스 등)에 들어있는 낱개 갯수.
     * 예: 콜라 1박스 = 24캔 -> 24
     */
    @Column(nullable = false)
    private int unitsPerPackage;

    @Builder
    public Product(String name, int price, String barcode, int unitsPerPackage) {
        this.name = name;
        this.price = price;
        this.barcode = barcode;
        this.unitsPerPackage = unitsPerPackage;
    }
}
