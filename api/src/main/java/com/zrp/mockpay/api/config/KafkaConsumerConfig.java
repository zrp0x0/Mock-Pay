package com.zrp.mockpay.api.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConsumerConfig {

    // 🌟 에러 핸들러 (재시도 + DLQ 이동)
    @Bean
    public CommonErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        
        // 1. 죽은 편지 발송자 (Dead Letter Publisher)
        // 실패한 메시지를 "원래토픽이름.DLT" 로 보냅니다.
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", record.partition()));

        // 2. 재시도 정책 (Backoff)
        // 1초 간격으로 최대 3번 시도합니다.
        FixedBackOff backOff = new FixedBackOff(1000L, 3L);

        // 3. 핸들러 조립 (3번 실패하면 -> recoverer가 DLT로 보냄)
        return new DefaultErrorHandler(recoverer, backOff);
    }
    
    // 🏭 컨테이너 팩토리 (위에서 만든 에러 핸들러를 적용)
    // 스프링 부트가 @KafkaListener를 찾아서 실행할 때 이 설정을 참고하게 합니다.
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            org.springframework.kafka.core.ConsumerFactory<String, String> consumerFactory,
            CommonErrorHandler commonErrorHandler) {
        
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(commonErrorHandler); // 👈 핵심!
        
        return factory;
    }
}