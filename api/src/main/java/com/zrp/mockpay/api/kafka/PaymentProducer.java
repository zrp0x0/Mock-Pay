package com.zrp.mockpay.api.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentProducer {

    // KafkaTemplate: 스프링이 제공하는 "Kafka 우체부"
    private final KafkaTemplate<String, String> kafkaTemplate;

    public PaymentProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    // 메시지 발송 메서드
    public void send(String topic, String message) {
        System.out.println("📣 [Kafka Producer] 전송 중... Topic: " + topic + ", Msg: " + message);
        
        // send(토픽이름, 메시지)
        kafkaTemplate.send(topic, message);
        
        System.out.println("✅ [Kafka Producer] 전송 완료!");
    }
}