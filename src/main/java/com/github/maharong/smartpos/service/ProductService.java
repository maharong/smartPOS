package com.github.maharong.smartpos.service;

import com.github.maharong.smartpos.dto.ProductCreateRequest;
import com.github.maharong.smartpos.dto.ProductResponse;
import com.github.maharong.smartpos.dto.ProductUpdateRequest;
import com.github.maharong.smartpos.entity.Product;
import com.github.maharong.smartpos.enums.ProductStatus;
import com.github.maharong.smartpos.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 상품 도메인의 비즈니스 로직을 처리하는 서비스.
 * <p>
 * 상품은 과거 판매/발주 이력과 연결될 수 있으므로 일반적으로 물리 삭제하지 않고,
 * {@link ProductStatus}로 운영 상태를 관리한다.
 * </p>
 *
 * <ul>
 *   <li>상품 기본 정보 수정은 {@link #update(Long, ProductUpdateRequest)}에서 처리한다.</li>
 *   <li>상태 변경은 별도 메서드({@link #discontinue(Long)}, {@link #activate(Long)})로 분리한다.</li>
 *   <li>바코드는 고유값으로 취급하며 등록 시 중복을 허용하지 않는다.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    /**
     * 상품을 등록한다.
     * <p>
     * 바코드는 상품을 식별하는 고유 값으로 취급하므로, 이미 등록된 바코드가 존재하면 등록을 거부한다.
     * </p>
     *
     * @param req 상품 등록 요청 DTO
     * @return 등록된 상품 응답 DTO
     * @throws IllegalArgumentException 바코드가 이미 존재하는 경우
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
     * 상품 ID로 상품을 조회한다.
     *
     * @param id 상품 ID
     * @return 상품 응답 DTO
     * @throws IllegalArgumentException 해당 ID의 상품이 존재하지 않는 경우
     */
    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. id=" + id));
        return toResponse(product);
    }

    /**
     * 바코드로 상품을 조회한다.
     *
     * @param barcode 바코드
     * @return 상품 응답 DTO
     * @throws IllegalArgumentException 해당 바코드의 상품이 존재하지 않는 경우
     */
    @Transactional(readOnly = true)
    public ProductResponse getByBarcode(String barcode) {
        Product product = productRepository.findByBarcode(barcode)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + barcode));
        return toResponse(product);
    }

    /**
     * 상품의 기본 정보를 수정한다.
     * <p>
     * 이 메서드는 상품 상태({@link ProductStatus}) 변경을 수행하지 않는다.
     * 상태 변경은 {@link #discontinue(Long)}, {@link #activate(Long)} 등 별도 API/메서드로 처리한다.
     * </p>
     *
     * @param id 상품 ID
     * @param req 수정 요청 DTO
     * @return 수정된 상품 응답 DTO
     * @throws IllegalArgumentException 해당 ID의 상품이 존재하지 않는 경우
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
     * 전체 상품 목록을 조회한다.
     * <p>
     * 상태와 관계없이 전체 상품을 반환한다.
     * </p>
     *
     * @return 상품 응답 DTO 목록
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getAll() {
        return productRepository.findAll()
                .stream()
                .map(ProductService::toResponse)
                .toList();
    }

    /**
     * 특정 상태의 상품 목록을 조회한다.
     *
     * @param status 조회할 상품 상태
     * @return 상품 응답 DTO 목록
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllByStatus(ProductStatus status) {
        return productRepository.findAllByStatus(status)
                .stream()
                .map(ProductService::toResponse)
                .toList();
    }

    /**
     * 상품을 단종 상태로 변경한다.
     * <p>
     * 단종은 운영상 "추천/발주/판매 대상에서 제외"에 가깝고, 이력 보존을 위해 물리 삭제 대신 상태값으로 관리한다.
     * </p>
     *
     * @param id 상품 ID
     * @return 상태가 변경된 상품 응답 DTO
     * @throws IllegalArgumentException 해당 ID의 상품이 존재하지 않는 경우
     */
    public ProductResponse discontinue(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. id=" + id));

        product.discontinue();
        return toResponse(product);
    }

    /**
     * 상품을 판매 가능 상태로 변경한다.
     * <p>
     * {@link ProductStatus#DISCONTINUED} 또는 {@link ProductStatus#PAUSED} 상태의 상품을
     * {@link ProductStatus#ACTIVE}로 전환하는 용도로 사용할 수 있다.
     * </p>
     *
     * @param id 상품 ID
     * @return 상태가 변경된 상품 응답 DTO
     * @throws IllegalArgumentException 해당 ID의 상품이 존재하지 않는 경우
     */
    public ProductResponse activate(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. id=" + id));

        product.activate();
        return toResponse(product);
    }

    /**
     * 상품을 발주 중단 상태로 변경한다.
     * <p>
     * 발주 추천/발주 대상에서 제외하고 싶을 때 사용한다.
     * </p>
     *
     * @param id 상품 ID
     * @return 상태가 변경된 상품 응답 DTO
     * @throws IllegalArgumentException 해당 ID의 상품이 존재하지 않는 경우
     */
    public ProductResponse pause(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. id=" + id));

        product.pause();
        return toResponse(product);
    }

    /**
     * {@link Product} 엔티티를 {@link ProductResponse}로 변환한다.
     *
     * @param product 상품 엔티티
     * @return 상품 응답 DTO
     */
    private static ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getBarcode(),
                product.getUnitsPerPackage(),
                product.getStatus()
        );
    }
}