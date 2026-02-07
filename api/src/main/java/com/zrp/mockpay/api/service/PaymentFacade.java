// package com.zrp.mockpay.api.service;

// import com.zrp.mockpay.api.dto.PaymentRequest;
// import com.zrp.mockpay.api.dto.PaymentResponse;
// import org.redisson.api.RLock;
// import org.redisson.api.RedissonClient;
// import org.springframework.stereotype.Component;

// import java.util.concurrent.TimeUnit;

// @Component
// public class PaymentFacade {

//     private final RedissonClient redissonClient;
//     private final PaymentService paymentService;

//     public PaymentFacade(RedissonClient redissonClient, PaymentService paymentService) {
//         this.redissonClient = redissonClient;
//         this.paymentService = paymentService;
//     }

//     // 결제 진입 전, 여기서 번호표(Lock)를 뽑습니다.
//     public PaymentResponse use(PaymentRequest request) {
//         // 1. 락 이름 정의 (회원 ID 별로 잠금)
//         // 예: "payment:lock:1" -> 1번 회원 거 건드리는 사람 다 줄 서!
//         String lockKey = "payment:lock:" + request.memberId();
//         RLock lock = redissonClient.getLock(lockKey);

//         try {
//             // 2. 락 획득 시도 (최대 5초 기다림, 획득 후 1초 동안 잡고 있음)
//             boolean available = lock.tryLock(5, TimeUnit.SECONDS);

//             if (!available) {
//                 throw new RuntimeException("현재 사용자가 너무 많습니다. 잠시 후 다시 시도해주세요.");
//             }

//             // 3. 락 획득 성공! -> 진짜 비즈니스 로직(Service) 호출
//             return paymentService.use(request);

//         } catch (InterruptedException e) {
//             throw new RuntimeException("인터럽트 발생");
//         } finally {
//             // 4. 무조건 락 반납 (이거 안 하면 영원히 락 걸림!)
//             if (lock.isHeldByCurrentThread()) {
//                 lock.unlock();
//             }
//         }
//     }
// }

//
package com.zrp.mockpay.api.service;

import com.zrp.mockpay.api.dto.PaymentRequest;
import com.zrp.mockpay.api.dto.PaymentResponse;
import com.zrp.mockpay.dbcore.aop.DistributedLock; // 우리가 만든 어노테이션
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.zrp.mockpay.api.kafka.PaymentProducer;

import java.util.concurrent.TimeUnit;

@Component
public class PaymentFacade {

    private final PaymentService paymentService;
    private final StringRedisTemplate redisTemplate; // Redis 도구 추가
    private final ObjectMapper objectMapper; // 객체 -> JSON 변환기
    private final PaymentProducer paymentProducer; // 카프카 확성기

    public PaymentFacade(PaymentService paymentService, StringRedisTemplate redisTemplate, ObjectMapper objectMapper, PaymentProducer paymentProducer) {
        this.paymentService = paymentService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.paymentProducer = paymentProducer;
    }

    // ✨ 깔끔 그 자체!
    // key: #request.memberId -> 파라미터 'request'의 'memberId' 필드를 키로 쓴다.
    // waitTime: 100초 (테스트용으로 길게 잡음)
    // @DistributedLock(key = "#request.memberId", waitTime = 100, timeUnit = TimeUnit.SECONDS)
    // public PaymentResponse use(PaymentRequest request) {
    //     return paymentService.use(request);
    // }

    // 멱등성
    @DistributedLock(key = "#request.memberId", waitTime = 100, timeUnit = TimeUnit.SECONDS)
    public PaymentResponse use(PaymentRequest request) {
        
        // 1. 멱등성 검증: "이미 처리된 주문번호인가?"
        String idempotencyKey = "pay:history:" + request.orderId();
        
        try {
            // Redis에서 조회
            String savedResponse = redisTemplate.opsForValue().get(idempotencyKey);
            
            if (savedResponse != null) {
                // ✌️ "어! 이거 아까 처리했어요. 여기 영수증(결과) 가져가세요."
                System.out.println("♻️ 중복 요청 감지! 저장된 응답 반환: " + request.orderId());
                return objectMapper.readValue(savedResponse, PaymentResponse.class);
            }

            // 2. 비즈니스 로직 실행 (실제 결제)
            PaymentResponse response = paymentService.use(request);

            // 3. 결과 저장 (Redis에 "처리 완료" 도장 찍기)
            // 24시간 동안 보관 (실무에선 정책에 따라 다름)
            String responseJson = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(idempotencyKey, responseJson, 24, TimeUnit.HOURS);

            paymentProducer.send("payment-topic", responseJson);

            return response;

        } catch (Exception e) {
            // JSON 변환 에러 등은 런타임 에러로 던짐
            throw new RuntimeException(e);
        }
    }
}