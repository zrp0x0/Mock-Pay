package com.zrp.mockpay.api.service;

import com.zrp.mockpay.api.dto.PaymentRequest;
import com.zrp.mockpay.api.dto.PaymentResponse;
import com.zrp.mockpay.api.service.PointService;
import com.zrp.mockpay.dbcore.entity.Member;
import com.zrp.mockpay.dbcore.entity.PaymentHistory;
import com.zrp.mockpay.dbcore.enums.PaymentType;
import com.zrp.mockpay.dbcore.repository.MemberRepository;
import com.zrp.mockpay.dbcore.repository.PaymentHistoryRepository;
import jakarta.transaction.Transactional; // ⚠️ org.springframework... 가 아니라 jakarta... 를 써도 되지만, 보통 스프링에선 org.springframework.transaction.annotation.Transactional을 씁니다. (아래 설명 참조)
import com.zrp.mockpay.api.dto.ChargeRequest;

import org.springframework.stereotype.Service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker; // 👈 import
import io.github.resilience4j.circuitbreaker.CallNotPermittedException; // 👈 import 추가

@Service // "나는 비즈니스 로직을 담당하는 직원(Service)이야"
public class PaymentService {

    private final MemberRepository memberRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final PointService pointService;    

    public PaymentService(MemberRepository memberRepository, PaymentHistoryRepository paymentHistoryRepository, PointService pointService) {
        this.memberRepository = memberRepository;
        this.paymentHistoryRepository = paymentHistoryRepository;
        this.pointService = pointService;
    }

    // 👇 [추가] 외부 은행 결제 시뮬레이션
    // "bankService"라는 설정(위에서 만든 yml)을 따르겠다.
    // 실패하면 fallback(대안) 메서드를 실행해라.
    @CircuitBreaker(name = "bankService", fallbackMethod = "payFallback")
    public String callForeignBank() {
        // 상황: 외부 은행이 계속 에러를 냄
        System.out.println("🏦 [Bank] 외부 은행 서버 호출 중...");
        throw new RuntimeException("은행 서버 다운됨!");
    }

    // 👇 [대안] 서킷이 열리거나 에러가 났을 때 실행될 메서드
    // 파라미터와 리턴 타입이 원본 메서드와 같아야 함 (+ 예외 파라미터)
    public String payFallback(Throwable t) {
        // 1. 서킷 브레이커가 차단한 경우 (OPEN 상태)
        if (t instanceof CallNotPermittedException) {
            System.out.println("⛔ [Circuit Breaker] 회로가 열려있습니다! (메서드 실행 아예 안 함)");
        } 
        // 2. 메서드 실행은 했는데 에러가 난 경우 (CLOSED 상태)
        else {
            System.out.println("🛡️ [Fallback] 에러 발생으로 인한 대체 로직: " + t.getMessage());
        }
        return "죄송합니다. 현재 은행 점검 중으로 나중에 시도해주세요.";
    }

    // 👇 [중요] Transactional: 이 메서드가 끝날 때까지 에러가 없어야 DB에 반영됨!
    // 하나라도 실패하면 없던 일로 되돌림 (Rollback)
    @org.springframework.transaction.annotation.Transactional
    public PaymentResponse charge(ChargeRequest request) {
        // 1. 손님 찾기 (없으면 에러)
        Member member = memberRepository.findById(request.memberId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 2. 잔액 충전 (Member 엔티티의 비즈니스 로직 사용)
        member.charge(request.amount());

        // 3. 영수증 기록
        PaymentHistory history = new PaymentHistory(member, request.amount(), PaymentType.CHARGE);
        paymentHistoryRepository.save(history);

        // 4. 결과 리턴
        return new PaymentResponse("충전 성공", member.getBalance());
    }

    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public PaymentResponse use(PaymentRequest request) {
        // 1. 손님 찾기 // 비관적 락
        // Member member = memberRepository.findByIdForUpdate(request.memberId())
        //         .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 분산 락 사용 중
        Member member = memberRepository.findById(request.memberId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 2. 잔액 사용 (잔액 부족하면 여기서 에러 터짐 -> 자동 롤백)
        member.use(request.amount());

        // 3. 영수증 기록
        PaymentHistory history = new PaymentHistory(member, request.amount(), PaymentType.USE);
        paymentHistoryRepository.save(history);

        // 테스트용 지뢰 강제로 예외 발생
        // 상황: DB에 저장은 다 했는데 마지막에 알 수 없는 에러가 터짐
        // if (true) {
        //     throw new RuntimeException("⚠ 긴급! 결제 마무리 중 에러 발생!");
        // }

        // Checked Error 테스트
        // if (true) {
        //     throw new Exception("체크드 예외 발생! 롤백이 안 될걸?");
        // }

        // 트랜잭션 전파 공부
        try {
            pointService.earnPoints(member.getId(), request.amount());
        } catch (Exception e) {
            System.out.println("⚠ 포인트 적립 실패! (하지만 결제는 진행함): " + e.getMessage());
        }

        // 4. 결과 리턴
        return new PaymentResponse("결제 성공", member.getBalance());
    }


}
