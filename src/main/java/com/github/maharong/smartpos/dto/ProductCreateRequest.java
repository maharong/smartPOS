package com.github.maharong.smartpos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ProductCreateRequest(
        @NotBlank String name,
        @Min(0) int price,
        @NotBlank String barcode,
        @Min(1) int unitsPerPackage
) {
}
