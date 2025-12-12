package com.github.maharong.smartpos.repository;

import com.github.maharong.smartpos.entity.Product;
import com.github.maharong.smartpos.entity.PurchaseOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Long> {

    List<PurchaseOrderItem> findByProduct(Product product);
}
