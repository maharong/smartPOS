package com.github.maharong.smartpos.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 특정 상품의 재고를 "입고 배치" 단위로 관리하는 엔티티.
 * <p>
 * 배치는 입고일({@link #receivedDate}), 유통기한({@link #expiryDate}), 현재 수량({@link #quantity})을 가진다.
 * 출고/판매 시에는 배치의 수량을 차감하며, 유통기한이 빠른 배치부터 차감하는 FEFO 정책을 적용할 수 있다.
 * </p>
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class InventoryBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 배치가 속한 상품.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    /**
     * 현재 배치 수량.
     * <p>
     * 수량은 음수가 될 수 없으며, {@link #increase(int)} / {@link #decrease(int)}로만 변경한다.
     * </p>
     */
    @Column(nullable = false)
    private int quantity;

    /**
     * 배치의 유통기한.
     */
    @Column(nullable = false)
    private LocalDate expiryDate;

    /**
     * 배치의 입고일.
     */
    @Column(nullable = false)
    private LocalDate receivedDate;

    /**
     * 배치를 생성한다.
     *
     * @param product 배치가 속한 상품
     * @param quantity 초기 수량(양수)
     * @param expiryDate 유통기한
     * @param receivedDate 입고일
     */
    @Builder
    public InventoryBatch(Product product, int quantity, LocalDate expiryDate, LocalDate receivedDate) {
        this.product = product;
        this.quantity = quantity;
        this.expiryDate = expiryDate;
        this.receivedDate = receivedDate;
    }

    /**
     * 배치 수량을 증가시킨다.
     *
     * @param amount 증가량(양수)
     * @throws IllegalArgumentException 증가량이 0 이하인 경우
     */
    public void increase(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("증가량은 0보다 커야합니다.");
        }
        this.quantity += amount;
    }

    /**
     * 배치 수량을 감소시킨다.
     * <p>
     * 수량은 음수가 될 수 없으며, 현재 수량보다 큰 감소량은 허용하지 않는다.
     * </p>
     *
     * @param amount 감소량(양수)
     * @throws IllegalArgumentException 감소량이 0 이하인 경우
     * @throws IllegalArgumentException 배치 수량이 부족한 경우
     */
    public void decrease(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("감소량은 0보다 커야합니다.");
        }
        if (this.quantity < amount) {
            throw new IllegalArgumentException("배치 수량이 충분하지 않습니다.");
        }
        this.quantity -= amount;
    }
}