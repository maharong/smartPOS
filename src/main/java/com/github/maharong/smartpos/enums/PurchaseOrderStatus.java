package com.github.maharong.smartpos.enums;

public enum PurchaseOrderStatus {
    REQUESTED,  // 발주 요청만 한 상태
    ORDERED,    // 실제 주문 접수 완료
    RECEIVED,   // 입고 완료
    CANCELED    // 발주 취소
}
