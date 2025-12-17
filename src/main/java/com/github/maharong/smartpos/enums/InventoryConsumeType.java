package com.github.maharong.smartpos.enums;

public enum InventoryConsumeType {
    ADJUSTMENT, // 관리자 수량 보정
    WASTE,      // 폐기(유통기한 만료 등)
    DAMAGE,     // 파손
    LOSS        // 분실, 도난 등
}
