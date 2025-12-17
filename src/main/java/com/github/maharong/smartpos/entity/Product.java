package com.github.maharong.smartpos.entity;

import com.github.maharong.smartpos.enums.ProductStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상품 정보를 담는 엔티티.
 * <p>
 * 상품은 판매/발주 등 과거 이력과 연결될 수 있으므로, 일반적으로 물리 삭제하지 않고
 * {@link ProductStatus}로 운영 상태를 관리한다.
 * </p>
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 상품 이름.
     */
    @Column(nullable = false)
    private String name;

    /**
     * 상품 가격.
     */
    @Column(nullable = false)
    private int price;

    /**
     * 상품 바코드(고유값).
     */
    @Column(nullable = false, unique = true)
    private String barcode;

    /**
     * 하나의 발주 단위(박스 등)에 들어있는 낱개 개수.
     */
    @Column(nullable = false)
    private int unitsPerPackage;

    /**
     * 상품 상태.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status = ProductStatus.ACTIVE;

    /**
     * 상품을 생성한다.
     * <p>
     * 상태({@link #status})는 기본적으로 {@link ProductStatus#ACTIVE}로 시작한다.
     * </p>
     *
     * @param name 상품 이름
     * @param price 상품 가격
     * @param barcode 바코드
     * @param unitsPerPackage 발주 단위당 낱개 개수
     */
    @Builder
    public Product(String name, int price, String barcode, int unitsPerPackage) {
        this.name = name;
        this.price = price;
        this.barcode = barcode;
        this.unitsPerPackage = unitsPerPackage;
    }

    /**
     * 상품의 기본 정보를 수정한다.
     * <p>
     * 이 메서드는 상태({@link #status})를 변경하지 않는다.
     * </p>
     *
     * @param name 상품 이름
     * @param price 상품 가격
     * @param unitsPerPackage 발주 단위당 낱개 개수
     */
    public void update(String name, int price, int unitsPerPackage) {
        this.name = name;
        this.price = price;
        this.unitsPerPackage = unitsPerPackage;
    }

    /**
     * 상품을 단종 상태로 변경한다.
     */
    public void discontinue() {
        this.status = ProductStatus.DISCONTINUED;
    }

    /**
     * 상품을 판매 가능 상태로 변경한다.
     * <p>
     * {@link ProductStatus#DISCONTINUED} 또는 {@link ProductStatus#PAUSED} 상태에서
     * {@link ProductStatus#ACTIVE}로 복귀시키는 용도로 사용할 수 있다.
     * </p>
     */
    public void activate() {
        this.status = ProductStatus.ACTIVE;
    }

    /**
     * 상품을 발주 중단 상태로 변경한다.
     * <p>
     * 단종은 아니지만 발주 추천/발주 대상에서 제외하고 싶을 때 사용한다.
     * </p>
     */
    public void pause() {
        this.status = ProductStatus.PAUSED;
    }
}