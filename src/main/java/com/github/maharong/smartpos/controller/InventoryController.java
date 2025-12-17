package com.github.maharong.smartpos.controller;

import com.github.maharong.smartpos.dto.*;
import com.github.maharong.smartpos.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 재고 관련 REST API를 제공하는 컨트롤러.
 * <p>
 * 재고는 배치({@link com.github.maharong.smartpos.entity.InventoryBatch}) 단위로 관리하며,
 * 배치 수량 변경(입고/출고/폐기)은 서비스 계층에서 처리한다.
 * </p>
 *
 * <ul>
 *   <li>입고는 배치를 생성한다.</li>
 *   <li>관리자 출고는 FEFO 기준으로 배치를 차감하고, 출고 이력을 로그로 남긴다.</li>
 *   <li>유통기한 만료 배치는 일괄 폐기 처리할 수 있다.</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * 입고 처리(배치 생성)를 수행한다.
     *
     * @param request 입고 요청 DTO
     * @return 생성된 배치 응답 DTO
     */
    @PostMapping("/receive")
    public InventoryBatchResponse receive(@Valid @RequestBody InventoryReceiveRequest request) {
        return inventoryService.receive(request);
    }

    /**
     * 관리자 출고 처리를 수행한다.
     * <p>
     * 출고는 FEFO 기준으로 배치 수량을 차감하며, 배치별 출고 로그를 남긴다.
     * </p>
     *
     * @param request 출고 요청 DTO
     */
    @PostMapping("/consume")
    public void consume(@Valid @RequestBody InventoryConsumeRequest request) {
        inventoryService.consume(request);
    }

    /**
     * 특정 상품의 배치 목록을 조회한다(유통기한 오름차순).
     *
     * @param productId 상품 ID
     * @return 배치 응답 목록
     */
    @GetMapping("/products/{productId}/batches")
    public List<InventoryBatchResponse> getBatches(@PathVariable Long productId) {
        return inventoryService.getBatches(productId);
    }

    /**
     * 특정 상품의 재고 요약을 조회한다.
     * <p>
     * 판매 가능 재고(수량이 남아있고, 만료되지 않은 배치)의 총합을 반환한다.
     * </p>
     *
     * @param productId 상품 ID
     * @return 재고 요약 응답 DTO
     */
    @GetMapping("/products/{productId}/summary")
    public InventorySummaryResponse getSummary(@PathVariable Long productId) {
        return inventoryService.getSummary(productId);
    }

    /**
     * 유통기한 임박/만료 배치를 조회한다.
     * <p>
     * {@code date} 파라미터가 없으면 오늘 날짜를 기준으로 조회한다.
     * </p>
     *
     * @param date 유통기한 비교 기준일(선택)
     * @return 유통기한 임박/만료 배치 목록
     */
    @GetMapping("/expiring")
    public List<ExpiringBatchResponse> getExpiringBatches(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        LocalDate baseDate = (date == null) ? LocalDate.now() : date;
        return inventoryService.getExpiringBatches(baseDate);
    }

    /**
     * 유통기한 만료 배치를 일괄 폐기 처리한다.
     * <p>
     * {@code date} 파라미터가 없으면 오늘 날짜를 기준으로 만료 여부를 판단한다.
     * </p>
     *
     * @param date 만료 판단 기준일(선택)
     * @param note 로그 메모(선택)
     * @return 일괄 폐기 처리 결과
     */
    @PostMapping("/dispose-expired")
    public DisposeExpiredResponse disposeExpired(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @RequestParam(required = false) String note
    ) {
        LocalDate baseDate = (date == null) ? LocalDate.now() : date;
        return inventoryService.disposeExpiredBatches(baseDate, note);
    }
}