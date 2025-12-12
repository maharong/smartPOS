package com.github.maharong.smartpos.repository;

import com.github.maharong.smartpos.entity.PurchaseOrder;
import com.github.maharong.smartpos.enums.PurchaseOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    List<PurchaseOrder> findByStatus(PurchaseOrderStatus status);
}
