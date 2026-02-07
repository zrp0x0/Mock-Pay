package com.zrp.mockpay.api.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentConsumer {

    // @KafkaListener: "저는 'payment-topic'이라는 대화방을 항상 듣고 있겠습니다."
    // groupId: "우리는 'payment-group'이라는 팀입니다." (팀 내에서 한 명만 듣게 할 때 사용)
    @KafkaListener(topics = "payment-topic", groupId = "payment-group")
    public void listen(String message) {
        System.out.println("👂 [Kafka Consumer] 메시지 수신 성공!");
        System.out.println("📩 내용: " + message);

        // 여기서 "오래 걸리는 작업"을 처리한다고 가정합니다.
        // try {
        //     // 1. 포인트 적립 (가정)
        //     System.out.println("   ✨ (뒷단 작업) 포인트 적립 중...");
        //     Thread.sleep(1000); // 1초 걸리는 척

        //     // 2. 알림 발송 (가정)
        //     System.out.println("   🔔 (뒷단 작업) 고객님께 카톡 발송 완료!");
            
        // } catch (InterruptedException e) {
        //     e.printStackTrace();
        // }

        if (true) {
            throw new RuntimeException("강제 에러 발생, 재시도 테스트 중");
        }
        

        System.out.println("처리 완료!!!");
    }

    // 👇 [추가] DLT(무덤) 감시자
    // 실패해서 쫓겨난 메시지가 여기로 오는지 확인합니다.
    @KafkaListener(topics = "payment-topic.DLT", groupId = "dlt-group")
    public void listenDeadLetter(String message) {
        System.out.println("💀 [DLQ Consumer] 에러 메시지 도착(영안실): " + message);
        // 여기서 관리자에게 알림을 보내거나, 별도 DB에 저장해서 나중에 수동 처리함.
    }

}