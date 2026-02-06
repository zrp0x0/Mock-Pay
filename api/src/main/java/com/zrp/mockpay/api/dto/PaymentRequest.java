package com.zrp.mockpay.api.dto;

// record: "데이터만 담는 그릇"을 만드는 최신 문법 (Getter, 생성자 자동 생성)
public record PaymentRequest(
    Long memberId,
    Long amount
) {}