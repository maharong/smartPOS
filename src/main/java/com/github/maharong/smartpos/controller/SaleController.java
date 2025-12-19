package com.github.maharong.smartpos.controller;

import com.github.maharong.smartpos.dto.SaleCreateRequest;
import com.github.maharong.smartpos.dto.SaleResponse;
import com.github.maharong.smartpos.entity.Sale;
import com.github.maharong.smartpos.entity.SaleItem;
import com.github.maharong.smartpos.repository.SaleItemRepository;
import com.github.maharong.smartpos.service.SaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 판매(영수증) API 컨트롤러.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/sales")
public class SaleController {

    private final SaleService saleService;
    private final SaleItemRepository saleItemRepository;

    /**
     * 판매 1건(영수증 1장)을 생성한다.
     *
     * <p>판매 생성은 다음을 한 트랜잭션으로 처리한다.</p>
     * <ul>
     *   <li>Sale(영수증) 저장</li>
     *   <li>SaleItem(판매 라인) 저장</li>
     *   <li>재고 FEFO 차감</li>
     *   <li>총액 및 결제 금액 반영</li>
     * </ul>
     *
     * @param req 판매 생성 요청 DTO
     * @return 생성된 판매 응답 DTO
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SaleResponse create(@RequestBody SaleCreateRequest req) {
        Sale sale = saleService.createSale(
                req.paymentMethod(),
                req.cashAmount(),
                req.cardAmount(),
                req.pointAmount(),
                req.lines().stream()
                        .map(l -> new SaleService.CreateSaleLine(l.productId(), l.quantity()))
                        .toList()
        );

        List<SaleItem> items = saleItemRepository.findBySaleId(sale.getId());
        return SaleResponse.from(sale, items);
    }

    /**
     * 판매 1건을 조회한다.
     *
     * @param saleId 판매 ID
     * @return 판매 응답 DTO
     */
    @GetMapping("/{saleId}")
    public SaleResponse get(@PathVariable Long saleId) {
        Sale sale = saleService.getSale(saleId);
        List<SaleItem> items = saleItemRepository.findBySaleId(saleId);
        return SaleResponse.from(sale, items);
    }

    /**
     * 판매를 환불 처리한다.
     *
     * <p>정책: 환불 상품은 재입고하지 않고 폐기 처리한다. 따라서 재고는 복구하지 않는다.</p>
     *
     * @param saleId 판매 ID
     */
    @PatchMapping("/{saleId}/refund")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void refund(@PathVariable Long saleId) {
        saleService.refundSale(saleId);
    }
}