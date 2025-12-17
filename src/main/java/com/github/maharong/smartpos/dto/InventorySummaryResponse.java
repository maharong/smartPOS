package com.github.maharong.smartpos.dto;

public record InventorySummaryResponse(
        Long productId,
        String productName,
        int totalQuantity
) {
}
