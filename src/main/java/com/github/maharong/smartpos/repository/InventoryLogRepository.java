package com.github.maharong.smartpos.repository;

import com.github.maharong.smartpos.entity.InventoryLog;
import com.github.maharong.smartpos.entity.Product;
import com.github.maharong.smartpos.enums.InventoryConsumeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long> {
    // 상품 기준 로그 조회
    List<InventoryLog> findByProductOrderByOccurredAtDesc(Product product);

    // 상품 + 사유 로그 조회
    List<InventoryLog> findByProductAndTypeOrderByOccurredAtDesc(
            Product product,
            InventoryConsumeType type
    );
}
