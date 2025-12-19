package com.github.maharong.smartpos.repository;

import com.github.maharong.smartpos.entity.Product;
import com.github.maharong.smartpos.entity.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

    List<SaleItem> findByProduct(Product product);

    /**
     * 특정 판매(Sale)에 속한 판매 라인(SaleItem) 목록을 조회한다.
     *
     * @param saleId 판매 ID
     * @return 해당 판매의 판매 라인 목록
     */
    List<SaleItem> findBySaleId(Long saleId);
}