package com.github.maharong.smartpos.dto;

import java.time.LocalDate;

public record ExpiringBatchResponse(
        Long batchId,
        Long productId,
        String productName,
        int quantity,
        LocalDate expiryDate,
        long daysToExpiry
) {
}
