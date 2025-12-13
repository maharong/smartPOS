package com.github.maharong.smartpos.controller;

import com.github.maharong.smartpos.dto.ProductCreateRequest;
import com.github.maharong.smartpos.dto.ProductUpdateRequest;
import com.github.maharong.smartpos.dto.ProductResponse;
import com.github.maharong.smartpos.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    /**
     * 상품 등록
     */
    @PostMapping
    public ProductResponse create(@Valid @RequestBody ProductCreateRequest request) {
        return productService.create(request);
    }

    /**
     * 상품 ID로 조회
     */
    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable Long id) {
        return productService.getById(id);
    }

    /**
     * 바코드로 조회
     */
    @GetMapping("/barcode/{barcode}")
    public ProductResponse getByBarcode(@PathVariable String barcode) {
        return productService.getByBarcode(barcode);
    }

    /**
     * 상품 전체 목록 조회
     */
    @GetMapping
    public List<ProductResponse> getAll() {
        return productService.getAll();
    }

    /**
     * 상품 정보 수정
     */
    @PutMapping("/{id}")
    public ProductResponse update(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest request
    ) {
        return productService.update(id, request);
    }
}