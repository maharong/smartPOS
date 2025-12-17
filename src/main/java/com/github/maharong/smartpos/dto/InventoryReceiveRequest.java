package com.github.maharong.smartpos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record InventoryReceiveRequest(
        @NotNull Long productId,
        @Min(1) int quantity,
        @NotNull LocalDate expiryDate,
        LocalDate receivedDate
) {
}
