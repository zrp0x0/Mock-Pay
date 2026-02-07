package com.zrp.mockpay.dbcore.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Redisson Distributed Lock Annotation
 * <p>
 * 실무 Tip: RetentionPolicy.RUNTIME이어야 JVM 실행 시점에 리플렉션으로 정보를 읽을 수 있습니다.
 * Target은 메서드 레벨에만 붙이도록 제한합니다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    // 1. 락의 키값 (SpEL 지원 예: "#request.id")
    String key();

    // 2. 락의 시간 단위 (기본: 초)
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    // 3. 락을 기다리는 시간 (기본: 5초)
    // 락 획득을 위해 대기하다가 이 시간이 지나면 예외를 던짐 (Give up)
    long waitTime() default 5L;

    // 4. 락을 임대하는 시간 (기본: 3초)
    // 이 시간이 지나면 락이 자동으로 풀림 (서버 다운 시 데드락 방지용)
    long leaseTime() default -1L;
}