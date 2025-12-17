package com.github.maharong.smartpos.repository;

import com.github.maharong.smartpos.entity.InventoryBatch;
import com.github.maharong.smartpos.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface InventoryBatchRepository extends JpaRepository<InventoryBatch, Long> {
    // FEFO(유통기한 빠른 순) 출고를 위한 배치 조회
    List<InventoryBatch> findByProductOrderByExpiryDateAsc(Product product);

    // 유통기한 임박 배치 조회 (<= date)
    List<InventoryBatch> findByExpiryDateLessThanEqual(LocalDate date);

    // 유통기한이 기준일보다 "이전"(expired) 이고, 수량이 남아있는 배치
    List<InventoryBatch> findByExpiryDateBeforeAndQuantityGreaterThan(LocalDate baseDate, int quantity);
}
