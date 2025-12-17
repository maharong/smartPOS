package com.github.maharong.smartpos.dto;

import com.github.maharong.smartpos.entity.InventoryBatch;

import java.time.LocalDate;

public record InventoryBatchResponse(
        Long batchId,
        Long productId,
        String productName,
        int quantity,
        LocalDate expiryDate,
        LocalDate receivedDate
) {
    public static InventoryBatchResponse from(InventoryBatch batch) {
        return new InventoryBatchResponse(
                batch.getId(),
                batch.getProduct().getId(),
                batch.getProduct().getName(),
                batch.getQuantity(),
                batch.getExpiryDate(),
                batch.getReceivedDate()
        );
    }
}
