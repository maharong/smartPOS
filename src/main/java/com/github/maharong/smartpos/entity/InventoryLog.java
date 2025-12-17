package com.github.maharong.smartpos.entity;

import com.github.maharong.smartpos.enums.InventoryConsumeType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 재고 변동(주로 출고/폐기/조정) 이력을 기록하는 엔티티.
 * <p>
 * 판매 이력은 {@code Sale}/{@code SaleItem}에서 별도로 관리하며,
 * 본 엔티티는 관리자 출고(폐기/파손/조정/분실 등) 중심의 로그를 남긴다.
 * </p>
 *
 * <ul>
 *   <li>어떤 상품({@link #product})의 변동인지 기록한다.</li>
 *   <li>가능하면 어떤 배치({@link #batch})에서 차감되었는지 함께 기록한다.</li>
 *   <li>변동 유형은 {@link InventoryConsumeType}으로 표현한다.</li>
 * </ul>
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class InventoryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 어떤 상품의 재고 변동인지.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    /**
     * 어떤 배치에서 차감되었는지(배치 단위 추적용).
     * <p>
     * 배치가 특정될 수 없는 경우 null일 수 있다.
     * </p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private InventoryBatch batch;

    /**
     * 재고 변동 유형.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InventoryConsumeType type;

    /**
     * 변동 수량.
     * <p>
     * 항상 양수로 저장한다(예: 차감 5는 {@code 5}로 저장).
     * </p>
     */
    @Column(nullable = false)
    private int quantity;

    /**
     * 관리자 메모(선택).
     */
    @Column
    private String note;

    /**
     * 로그 발생 시각.
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    /**
     * 재고 로그를 생성한다.
     * <p>
     * {@link #occurredAt}는 서비스에서 지정하거나, 엔티티 라이프사이클 콜백으로 자동 지정할 수 있다.
     * </p>
     *
     * @param product 대상 상품
     * @param batch 대상 배치(선택)
     * @param type 변동 유형
     * @param quantity 변동 수량(양수)
     * @param note 관리자 메모(선택)
     * @param occurredAt 발생 시각(선택)
     */
    @Builder
    public InventoryLog(
            Product product,
            InventoryBatch batch,
            InventoryConsumeType type,
            int quantity,
            String note,
            LocalDateTime occurredAt
    ) {
        this.product = product;
        this.batch = batch;
        this.type = type;
        this.quantity = quantity;
        this.note = note;
        this.occurredAt = occurredAt;
    }
}