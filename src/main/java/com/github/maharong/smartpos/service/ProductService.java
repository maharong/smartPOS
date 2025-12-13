package com.github.maharong.smartpos.service;

import com.github.maharong.smartpos.dto.ProductCreateRequest;
import com.github.maharong.smartpos.dto.ProductResponse;
import com.github.maharong.smartpos.dto.ProductUpdateRequest;
import com.github.maharong.smartpos.entity.Product;
import com.github.maharong.smartpos.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {
    private final ProductRepository productRepository;

    /**
     * 상품 등록
     */
    public ProductResponse create(ProductCreateRequest req) {
        if (productRepository.existsByBarcode(req.barcode())) {
            throw new IllegalArgumentException("이미 등록된 바코드입니다: " + req.barcode());
        }

        Product product = Product.builder()
                .name(req.name())
                .price(req.price())
                .barcode(req.barcode())
                .unitsPerPackage(req.unitsPerPackage())
                .build();

        Product saved = productRepository.save(product);
        return toResponse(saved);
    }

    /**
     * 상품 ID로 조회
     */
    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. id=" + id));
        return toResponse(product);
    }

    /**
     * 바코드로 조회
     */
    @Transactional(readOnly = true)
    public ProductResponse getByBarcode(String barcode) {
        Product product = productRepository.findByBarcode(barcode)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + barcode));
        return toResponse(product);
    }

    /**
     * 상품 정보 수정
     */
    public ProductResponse update(Long id, ProductUpdateRequest req) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. id=" + id));

        product.update(
                req.name(),
                req.price(),
                req.unitsPerPackage()
        );

        return toResponse(product);
    }

    /**
     * 전체 상품 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getAll() {
        return productRepository.findAll()
                .stream()
                .map(ProductService::toResponse)
                .toList();
    }

    private static ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getBarcode(),
                product.getUnitsPerPackage()
        );
    }
}
