package com.zrp.mockpay.dbcore.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AopForTransaction {

    // 부모 트랜잭션이 있어도 무조건 새로운 트랜잭션을 만듦 (락 점유 시간 최소화 목적도 있음)
    // 사실 Facade에서 락을 걸기 때문에 이 클래스 없이도 동작은 하지만, 
    // 구조적 안전성을 위해 분리하는 것이 Best Practice입니다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Object proceed(ProceedingJoinPoint joinPoint) throws Throwable {
        return joinPoint.proceed();
    }
}