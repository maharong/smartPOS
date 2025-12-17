package com.github.maharong.smartpos.dto;

import com.github.maharong.smartpos.enums.InventoryConsumeType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InventoryConsumeRequest(
        @NotNull Long productId,
        @Min(1) int quantity,
        @NotNull InventoryConsumeType type,
        String note // 관리자 메모
) {
}
