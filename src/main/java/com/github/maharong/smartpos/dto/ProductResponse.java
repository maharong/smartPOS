package com.github.maharong.smartpos.dto;

public record ProductResponse(
        Long id,
        String name,
        int price,
        String barcode,
        int unitsPerPackage
) {
}
