package com.github.maharong.smartpos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ProductUpdateRequest(
        @NotBlank String name,
        @Min(0) int price,
        @Min(1) int unitsPerPackage
) {
}
