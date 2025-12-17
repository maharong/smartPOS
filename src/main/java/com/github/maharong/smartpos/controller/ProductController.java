package com.github.maharong.smartpos.controller;

import com.github.maharong.smartpos.dto.ProductCreateRequest;
import com.github.maharong.smartpos.dto.ProductResponse;
import com.github.maharong.smartpos.dto.ProductUpdateRequest;
import com.github.maharong.smartpos.enums.ProductStatus;
import com.github.maharong.smartpos.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 상품 관련 REST API를 제공하는 컨트롤러.
 * <p>
 * 상품은 과거 이력(판매/발주 등)과 연결될 수 있으므로, 삭제 대신 {@link ProductStatus}로 상태를 관리한다.
 * </p>
 *
 * <ul>
 *   <li>상품 기본 정보 수정은 {@code PUT /products/{id}}에서 처리한다.</li>
 *   <li>상품 상태 변경은 별도 엔드포인트({@code /discontinue}, {@code /activate})로 분리한다.</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    /**
     * 상품을 등록한다.
     *
     * @param request 상품 등록 요청 DTO
     * @return 등록된 상품 응답 DTO
     */
    @PostMapping
    public ProductResponse create(@Valid @RequestBody ProductCreateRequest request) {
        return productService.create(request);
    }

    /**
     * 상품 ID로 상품을 조회한다.
     *
     * @param id 상품 ID
     * @return 상품 응답 DTO
     */
    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable Long id) {
        return productService.getById(id);
    }

    /**
     * 바코드로 상품을 조회한다.
     *
     * @param barcode 바코드
     * @return 상품 응답 DTO
     */
    @GetMapping("/barcode/{barcode}")
    public ProductResponse getByBarcode(@PathVariable String barcode) {
        return productService.getByBarcode(barcode);
    }

    /**
     * 상품 목록을 조회한다.
     * <p>
     * {@code status} 파라미터가 없으면 전체 상품을 반환하고,
     * 지정되면 해당 상태의 상품만 반환한다.
     * </p>
     *
     * @param status 상품 상태 필터(선택)
     * @return 상품 응답 DTO 목록
     */
    @GetMapping
    public List<ProductResponse> getAll(@RequestParam(required = false) ProductStatus status) {
        return (status == null)
                ? productService.getAll()
                : productService.getAllByStatus(status);
    }

    /**
     * 상품의 기본 정보를 수정한다.
     * <p>
     * 이 엔드포인트는 상품 상태({@link ProductStatus})를 변경하지 않는다.
     * 상태 변경은 {@link #discontinue(Long)} 또는 {@link #activate(Long)}를 사용한다.
     * </p>
     *
     * @param id 상품 ID
     * @param request 상품 수정 요청 DTO
     * @return 수정된 상품 응답 DTO
     */
    @PutMapping("/{id}")
    public ProductResponse update(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest request
    ) {
        return productService.update(id, request);
    }

    /**
     * 상품을 단종 상태로 변경한다.
     *
     * @param id 상품 ID
     * @return 상태가 변경된 상품 응답 DTO
     */
    @PatchMapping("/{id}/discontinue")
    public ProductResponse discontinue(@PathVariable Long id) {
        return productService.discontinue(id);
    }

    /**
     * 상품을 판매 가능 상태로 변경한다.
     *
     * @param id 상품 ID
     * @return 상태가 변경된 상품 응답 DTO
     */
    @PatchMapping("/{id}/activate")
    public ProductResponse activate(@PathVariable Long id) {
        return productService.activate(id);
    }

    /**
     * 상품을 발주 중단 상태로 변경한다.
     *
     * @param id 상품 ID
     * @return 상태가 변경된 상품 응답 DTO
     */
    @PatchMapping("/{id}/pause")
    public ProductResponse pause(@PathVariable Long id) {
        return productService.pause(id);
    }

}