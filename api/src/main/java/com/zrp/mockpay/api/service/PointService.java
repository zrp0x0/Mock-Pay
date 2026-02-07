package com.zrp.mockpay.api.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PointService {

    // 여기가 핵심! "나는 부모와 상관없이 새 트랜잭션을 만든다"
    // REQUIRES_NEW: 이미 트랜잭션이 있어도, 무시하고 새로 만듦.
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void earnPoints(Long memberId, Long amount) {
        // 포인트 적립 로직이 들어갈 자리...
        
        // 하지만 테스트를 위해 강제로 폭탄을 터뜨립니다!
        // throw new RuntimeException("⚠ 포인트 서버 접속 불가! (적립 실패)");
        System.out.println("포인트 서버 적립 성공");
    }
    
}