package com.zrp.mockpay.api.service;

import com.zrp.mockpay.api.dto.PaymentRequest;
import com.zrp.mockpay.dbcore.entity.Member;
import com.zrp.mockpay.dbcore.repository.MemberRepository;
import com.zrp.mockpay.dbcore.repository.PaymentHistoryRepository;

import com.zrp.mockpay.api.service.PaymentService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class PaymentTransactionTest {

    @Autowired
    private PaymentService paymentService; // 여기선 Facade 말고 Service를 직접 테스트해도 됩니다.

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PaymentHistoryRepository paymentHistoryRepository;

    // @Test
    // @DisplayName("결제 중 예외가 발생하면, 차감된 잔액은 롤백되어야 한다.")
    // void rollback_test() throws Exception {
    //     paymentHistoryRepository.deleteAll();
    //     memberRepository.deleteAll();
        

    //     // 1. 준비
    //     Member member = new Member("RollbackTester", "rollback@test.com");
    //     member.charge(10000L); // 1만원 충전
    //     memberRepository.save(member);

    //     PaymentRequest request = new PaymentRequest(member.getId(), 2000L);

    //     // 2. 실행 (예외가 터져야 정상)
    //     // assertThatThrownBy(() -> paymentService.use(request))
    //     //         .isInstanceOf(RuntimeException.class)
    //     //         .hasMessage("⚠ 긴급! 결제 마무리 중 에러 발생!");

    //     assertThatThrownBy(() -> paymentService.use(request))
    //             .isInstanceOf(Exception.class)
    //             .hasMessage("체크드 예외 발생! 롤백이 안 될걸?");

    //     // 3. 검증 (가장 중요!)
    //     // 에러가 나서 튕겨 나갔으니, 잔액은 깎이지 않고 10,000원 그대로여야 함.
    //     Member findMember = memberRepository.findById(member.getId()).orElseThrow();
        
    //     System.out.println("========================================");
    //     System.out.println("기대 잔액: 10000원");
    //     System.out.println("실제 잔액: " + findMember.getBalance() + "원");
    //     System.out.println("========================================");

    //     assertThat(findMember.getBalance()).isEqualTo(10000L);
    // }

    @Test
    @DisplayName("포인트 적립이 실패해도(서브 트랜잭션 롤백), 잔액 차감(메인 트랜잭션)은 성공해야 한다.")
    void partial_success_test() {
        // 1. 준비
        Member member = new Member("PartialTester", "partial@test.com");
        member.charge(10000L); // 1만원 충전
        memberRepository.save(member);

        PaymentRequest request = new PaymentRequest(member.getId(), 2000L);

        // 2. 실행
        // (내부에서 PointService가 에러를 뿜지만, try-catch로 삼켜서 무시함)
        paymentService.use(request);

        // 3. 검증
        Member findMember = memberRepository.findById(member.getId()).orElseThrow();
        
        System.out.println("========================================");
        System.out.println("기대 잔액: 8000원 (결제 성공)");
        System.out.println("실제 잔액: " + findMember.getBalance() + "원");
        System.out.println("========================================");

        // 여기가 통과하면, 당신은 '트랜잭션 분리'를 마스터한 것입니다!
        assertThat(findMember.getBalance()).isEqualTo(8000L);
    }
}