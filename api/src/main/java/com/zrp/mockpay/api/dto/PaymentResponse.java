package com.zrp.mockpay.api.dto;

public record PaymentResponse(
    String result,  // "성공", "실패" 메시지
    Long balance    // 거래 후 잔액
) {}