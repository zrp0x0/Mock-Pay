package com.zrp.mockpay.dbcore.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * @Aspect: AOP 모듈임을 명시
 * @Component: 스프링 빈으로 등록
 */
@Aspect
@Component
public class DistributedLockAop {

    private static final String REDISSON_LOCK_PREFIX = "LOCK:";
    private static final Logger log = LoggerFactory.getLogger(DistributedLockAop.class);

    private final RedissonClient redissonClient;
    private final AopForTransaction aopForTransaction;

    public DistributedLockAop(RedissonClient redissonClient, AopForTransaction aopForTransaction) {
        this.redissonClient = redissonClient;
        this.aopForTransaction = aopForTransaction;
    }

    // @Around: 메서드 실행 전후를 모두 제어 (가장 강력한 어드바이스)
    // 포인트컷: @DistributedLock 어노테이션이 붙은 메서드만 타겟팅
    @Around("@annotation(com.zrp.mockpay.dbcore.aop.DistributedLock)")
    public Object lock(ProceedingJoinPoint joinPoint) throws Throwable {
        
        // 1. 어노테이션 정보 가져오기 (Reflection)
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        DistributedLock distributedLock = method.getAnnotation(DistributedLock.class);

        // 2. 키 파싱 (SpEL을 이용해 동적 키 생성)
        String key = REDISSON_LOCK_PREFIX + CustomSpringELParser.getDynamicValue(
                signature.getParameterNames(), // ex. request
                joinPoint.getArgs(), // PaymentRequest객체
                distributedLock.key() // "#request.id"
        );

        // 3. 락 객체 생성
        RLock rLock = redissonClient.getLock(key);

        try {
            // 4. 락 획득 시도
            log.info("Lock 획득 시도: {}", key);
            boolean available = rLock.tryLock(
                    distributedLock.waitTime(), 
                    distributedLock.leaseTime(),
                    distributedLock.timeUnit()
            );

            if (!available) {
                // 락 획득 실패 시 처리 (재시도 로직을 넣거나 예외 발생)
                log.warn("Lock 획득 실패: {}", key);
                return false; 
            }

            // 5. 락 획득 성공 -> 트랜잭션과 비즈니스 로직 실행
            // [심화] 여기서 바로 joinPoint.proceed()를 하지 않고 별도 클래스로 분리한 이유?
            // 부모 트랜잭션 유무와 관계없이 확실하게 커밋된 후 락을 풀기 위함입니다.
            log.info("Lock 획득 성공: {}", key);
            return aopForTransaction.proceed(joinPoint);

        } catch (InterruptedException e) {
            throw new InterruptedException();
        } finally {
            try {
                // 6. 락 해제 (반드시 finally 블록에서)
                rLock.unlock();
                log.info("Lock 해제: {}", key);
            } catch (IllegalMonitorStateException e) {
                // 이미 락이 풀렸거나, 내 락이 아닌 경우 (크게 신경 안 써도 됨 로그만)
                log.info("Lock 이미 해제됨 or 만료됨: {}", key);
            }
        }
    }
}