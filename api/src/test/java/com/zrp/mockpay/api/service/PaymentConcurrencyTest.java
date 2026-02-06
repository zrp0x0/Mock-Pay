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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest // "스프링 서버를 실제로 띄워서 테스트할게"
class PaymentConcurrencyTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PaymentHistoryRepository paymentHistoryRepository;

    @Test
    @DisplayName("따닥! 100명이 동시에 1,000원씩 결제하면?")
    void concurrency_test() throws InterruptedException {
        paymentHistoryRepository.deleteAll();
        memberRepository.deleteAll();

        // 1. 준비: 잔액 10,000원인 멤버 생성
        Member member = new Member("Tester", "test@concurrent.com");
        member.charge(1000000L); // 1만원 충전
        memberRepository.save(member);

        // 2. 동시성 환경 세팅 (100명의 닌자 고용)
        int threadCount = 150;
        // ExecutorService: 병렬 작업을 도와주는 자바의 도구
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        // CountDownLatch: 100명이 다 끝날 때까지 기다리게 하는 스톱워치
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 3. 100명이 동시에 1,000원 결제 시도!
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    PaymentRequest request = new PaymentRequest(member.getId(), 1000L);
                    paymentService.use(request); // 결제 시도!
                } finally {
                    latch.countDown(); // "저 다 했어요!"
                }
            });
        }

        // 4. 모든 요청이 끝날 때까지 대기
        latch.await();

        // 5. 검증 (결과 확인)
        Member findMember = memberRepository.findById(member.getId()).orElseThrow();
        
        System.out.println("========================================");
        System.out.println("최종 잔액: " + findMember.getBalance() + "원");
        System.out.println("========================================");

        // 기대 결과: 1,000,000원 있는데 1,000원씩 150번 썼으니...
        // 실제 결과 850,000이 나와야함 / 근데 지금은 안나오게 하는게 테스트 성공
        assertThat(findMember.getBalance()).isNotEqualTo(850000L);
    }
}