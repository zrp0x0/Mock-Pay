// package com.zrp.mockpay.api.service;

// import com.zrp.mockpay.api.dto.PaymentRequest;
// import com.zrp.mockpay.dbcore.entity.Member;
// import com.zrp.mockpay.dbcore.repository.MemberRepository;
// import com.zrp.mockpay.dbcore.repository.PaymentHistoryRepository;

// import com.zrp.mockpay.api.service.PaymentService;
// import com.zrp.mockpay.api.service.PaymentFacade;

// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.context.SpringBootTest;

// import java.util.concurrent.CountDownLatch;
// import java.util.concurrent.ExecutorService;
// import java.util.concurrent.Executors;

// import static org.assertj.core.api.Assertions.assertThat;

// @SpringBootTest // "스프링 서버를 실제로 띄워서 테스트할게"
// class PaymentConcurrencyTest {

//     @Autowired
//     private PaymentFacade paymentFacade;

//     @Autowired
//     private MemberRepository memberRepository;

//     @Autowired
//     private PaymentHistoryRepository paymentHistoryRepository;

//     @Test
//     @DisplayName("따닥! 100명이 동시에 1,000원씩 결제하면?")
//     void concurrency_test() throws InterruptedException {
//         paymentHistoryRepository.deleteAll();
//         memberRepository.deleteAll();

//         // 1. 준비: 잔액 10,000원인 멤버 생성
//         Member member = new Member("Tester", "test@concurrent.com");
//         member.charge(1000000L); // 1만원 충전
//         memberRepository.save(member);

//         // 2. 동시성 환경 세팅 (100명의 닌자 고용)
//         int threadCount = 150;
//         // ExecutorService: 병렬 작업을 도와주는 자바의 도구
//         ExecutorService executorService = Executors.newFixedThreadPool(32);
//         // CountDownLatch: 100명이 다 끝날 때까지 기다리게 하는 스톱워치
//         CountDownLatch latch = new CountDownLatch(threadCount);

//         // 3. 100명이 동시에 1,000원 결제 시도!
//         for (int i = 0; i < threadCount; i++) {
//             executorService.submit(() -> {
//                 try {
//                     PaymentRequest request = new PaymentRequest(member.getId(), 1000L);
//                     // paymentService.use(request); // 결제 시도!
//                     paymentFacade.use(request); // redis 분산 락 사용
//                 } finally {
//                     latch.countDown(); // "저 다 했어요!"
//                 }
//             });
//         }

//         // 4. 모든 요청이 끝날 때까지 대기
//         latch.await();

//         // 5. 검증 (결과 확인)
//         Member findMember = memberRepository.findById(member.getId()).orElseThrow();
        
//         System.out.println("========================================");
//         System.out.println("최종 잔액: " + findMember.getBalance() + "원");
//         System.out.println("========================================");

//         // 기대 결과: 1,000,000원 있는데 1,000원씩 150번 썼으니...
//         // 실제 결과 850,000이 나와야함 / 근데 지금은 안나오게 하는게 테스트 성공
//         assertThat(findMember.getBalance()).isEqualTo(850000L);
//     }
// }

// // 2번째 성공한 횟수 만큼 돈이 빠져나갔는지가 중요함
// // package com.zrp.mockpay.api.service;

// // import com.zrp.mockpay.api.dto.PaymentRequest;
// // import com.zrp.mockpay.dbcore.entity.Member;
// // import com.zrp.mockpay.dbcore.repository.MemberRepository;
// // import com.zrp.mockpay.dbcore.repository.PaymentHistoryRepository;
// // import org.junit.jupiter.api.DisplayName;
// // import org.junit.jupiter.api.Test;
// // import org.springframework.beans.factory.annotation.Autowired;
// // import org.springframework.boot.test.context.SpringBootTest;

// // import java.util.concurrent.CountDownLatch;
// // import java.util.concurrent.ExecutorService;
// // import java.util.concurrent.Executors;
// // import java.util.concurrent.atomic.AtomicInteger;

// // import static org.assertj.core.api.Assertions.assertThat;

// // @SpringBootTest
// // class PaymentConcurrencyTest {

// //     @Autowired
// //     private PaymentFacade paymentFacade; // Facade 주입

// //     @Autowired
// //     private MemberRepository memberRepository;

// //     @Autowired
// //     private PaymentHistoryRepository paymentHistoryRepository;

// //     @Test
// //     @DisplayName("Redis 락: 100만 원 있는데 150명이 동시에 1,000원 결제")
// //     void concurrency_test() throws InterruptedException {
// //         // 청소
// //         paymentHistoryRepository.deleteAll();
// //         memberRepository.deleteAll();

// //         // 1. 준비
// //         Member member = new Member("Tester", "test@concurrent.com");
// //         long initialBalance = 1000000L;
// //         member.charge(initialBalance);
// //         memberRepository.save(member);

// //         // 2. 동시성 환경 세팅
// //         int threadCount = 150;
// //         ExecutorService executorService = Executors.newFixedThreadPool(32);
// //         CountDownLatch latch = new CountDownLatch(threadCount);

// //         // 성공한 횟수를 세기 위한 카운터 (동시성 환경에서도 안전한 Integer)
// //         AtomicInteger successCount = new AtomicInteger(0);

// //         // 3. 공격 시작
// //         for (int i = 0; i < threadCount; i++) {
// //             executorService.submit(() -> {
// //                 try {
// //                     PaymentRequest request = new PaymentRequest(member.getId(), 1000L);
// //                     paymentFacade.use(request);
                    
// //                     // 에러가 안 나고 여기까지 왔다면 성공! 카운트 증가
// //                     successCount.incrementAndGet();
// //                 } catch (Exception e) {
// //                     System.out.println("결제 실패: " + e.getMessage());
// //                 } finally {
// //                     latch.countDown();
// //                 }
// //             });
// //         }

// //         latch.await();

// //         // 4. 검증
// //         Member findMember = memberRepository.findById(member.getId()).orElseThrow();
// //         long expectedBalance = initialBalance - (successCount.get() * 1000L);

// //         System.out.println("========================================");
// //         System.out.println("성공한 횟수: " + successCount.get() + "회");
// //         System.out.println("기대 잔액: " + expectedBalance + "원");
// //         System.out.println("실제 잔액: " + findMember.getBalance() + "원");
// //         System.out.println("========================================");

// //         // 성공한 횟수만큼 정확히 돈이 빠졌는가?
// //         assertThat(findMember.getBalance()).isEqualTo(expectedBalance);
// //     }
// // }