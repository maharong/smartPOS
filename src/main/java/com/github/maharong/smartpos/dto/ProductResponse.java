package com.github.maharong.smartpos.dto;

import com.github.maharong.smartpos.enums.ProductStatus;

public record ProductResponse(
        Long id,
        String name,
        int price,
        String barcode,
        int unitsPerPackage,
        ProductStatus status
) {
}
