package com.github.maharong.smartpos.repository;

import com.github.maharong.smartpos.entity.Product;
import com.github.maharong.smartpos.entity.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

    List<SaleItem> findByProduct(Product product);
}