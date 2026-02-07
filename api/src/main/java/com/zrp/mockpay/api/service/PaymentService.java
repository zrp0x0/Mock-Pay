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
import org.springframework.stereotype.Service;

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

    // 👇 [중요] Transactional: 이 메서드가 끝날 때까지 에러가 없어야 DB에 반영됨!
    // 하나라도 실패하면 없던 일로 되돌림 (Rollback)
    @org.springframework.transaction.annotation.Transactional
    public PaymentResponse charge(PaymentRequest request) {
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
