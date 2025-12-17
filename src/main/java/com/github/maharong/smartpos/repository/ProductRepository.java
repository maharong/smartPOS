package com.github.maharong.smartpos.repository;

import com.github.maharong.smartpos.entity.Product;
import com.github.maharong.smartpos.enums.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByBarcode(String barcode); // 바코드 조회
    boolean existsByBarcode(String barcode); // 바코드로 조회해서 존재하는 상품인지 여부
    List<Product> findAllByStatus(ProductStatus status); // 상품 상태별 조회(판매/단종)
}
