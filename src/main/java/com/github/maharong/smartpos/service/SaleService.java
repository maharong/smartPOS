package com.github.maharong.smartpos.service;

import com.github.maharong.smartpos.entity.Product;
import com.github.maharong.smartpos.entity.Sale;
import com.github.maharong.smartpos.entity.SaleItem;
import com.github.maharong.smartpos.enums.PaymentMethod;
import com.github.maharong.smartpos.enums.ProductStatus;
import com.github.maharong.smartpos.enums.SaleStatus;
import com.github.maharong.smartpos.repository.ProductRepository;
import com.github.maharong.smartpos.repository.SaleItemRepository;
import com.github.maharong.smartpos.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SaleService {

    private final ProductRepository productRepository;
    private final SaleItemRepository saleItemRepository;
    private final InventoryService inventoryService;
    private final SaleRepository saleRepository;

    /**
     * 판매 1건(영수증 1장)을 생성하고, 판매 라인 저장 + 재고(FEFO) 차감을 한 트랜잭션으로 처리한다.
     *
     * @param paymentMethod 결제 수단
     * @param lines 판매 라인 목록 (productId, quantity)
     * @return 생성된 Sale
     * @throws IllegalArgumentException 요청 값이 잘못된 경우
     * @throws RuntimeException 재고 부족 등으로 판매를 진행할 수 없는 경우(전체 롤백)
     */
    @Transactional
    public Sale createSale(
            PaymentMethod paymentMethod,
            long cashAmount,
            long cardAmount,
            long pointAmount,
            List<CreateSaleLine> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("판매 라인이 비어있습니다.");
        }

        LocalDateTime soldAt = LocalDateTime.now();

        // Sale 생성
        Sale sale = Sale.builder()
                .soldAt(soldAt)
                .totalPrice(0L) // 아래에서 계산 후 반영
                .paymentMethod(paymentMethod)
                .status(SaleStatus.COMPLETED)
                .build();

        saleRepository.save(sale);

        long totalPrice = 0L;
        List<SaleItem> saleItems = new ArrayList<>();

        for (CreateSaleLine line : lines) {
            if (line.quantity() <= 0) {
                throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");
            }

            Product product = productRepository.findById(line.productId())
                    .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. id=" + line.productId()));

            if (product.getStatus() != ProductStatus.ACTIVE) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "판매할 수 없는 상품 상태입니다. productId=" + product.getId() + ", status=" + product.getStatus()
                );
            }

            // 재고 차감
            inventoryService.consumeForSale(product, line.quantity(), soldAt.toLocalDate());

            int unitPrice = product.getPrice(); // 판매 단가를 상품 현재가로 고정
            SaleItem saleItem = SaleItem.builder()
                    .sale(sale)
                    .product(product)
                    .quantity(line.quantity())
                    .unitPrice(unitPrice)
                    .build();

            saleItems.add(saleItem);
            totalPrice += (long) unitPrice * line.quantity();
        }

        saleItemRepository.saveAll(saleItems);

        // 총액 반영
        sale.changeTotalPrice(totalPrice);

        long paidSum = cashAmount + cardAmount + pointAmount;
        if (paidSum != totalPrice) {
            throw new IllegalArgumentException(
                    "결제 금액 합이 총액과 일치하지 않습니다. total=" + totalPrice + ", paid=" + paidSum
            );
        }

        sale.changePaymentAmounts(cashAmount, cardAmount, pointAmount);

        return sale;
    }

    /**
     * 판매 조회.
     */
    @Transactional(readOnly = true)
    public Sale getSale(Long saleId) {
        return saleRepository.findById(saleId)
                .orElseThrow(() -> new IllegalArgumentException("판매 없음 id=" + saleId));
    }

    /**
     * 판매 생성 요청 라인.
     *
     * @param productId 상품 ID
     * @param quantity 수량
     */
    public record CreateSaleLine(Long productId, int quantity) {}

    /**
     * 판매를 환불 처리한다.
     *
     * <p>정책: 환불 상품은 재입고하지 않고 폐기 처리한다. 따라서 재고는 복구하지 않는다.</p>
     *
     * @param saleId 판매 ID
     * @throws IllegalArgumentException 판매를 찾을 수 없는 경우
     * @throws ResponseStatusException 이미 환불된 판매이거나 환불 불가 상태인 경우
     */
    @Transactional
    public void refundSale(Long saleId) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new IllegalArgumentException("판매 없음 id=" + saleId));

        if (sale.getStatus() == SaleStatus.REFUNDED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 환불 처리된 판매입니다. saleId=" + saleId);
        }

        // 환불 처리(재고 복구 없음 = 폐기 정책)
        sale.changeStatus(SaleStatus.REFUNDED);
    }
}
