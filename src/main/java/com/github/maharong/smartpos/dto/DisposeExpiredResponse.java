package com.github.maharong.smartpos.dto;

import java.time.LocalDate;

public record DisposeExpiredResponse(
        LocalDate baseDate,
        int batchCount,
        int totalDisposedQuantity
) {
}
