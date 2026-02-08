# 0. 프로젝트 목표
- 아래 문서는 이 프로젝트의 전체적인 학습 내용을 정리한 내용입니다.
- 더 자세한 내용(프로젝트 분리법, IDE 오류, 기술적인 이론적인 내용들)을 확인하고 싶으신 분은 project_memo.md를 확인해주세요.

---

# 💳 Mock-Pay: 분산 환경을 고려한 결제 시스템 기술 다이브

> **“1번 요청하나, 100번 요청하나 결과는 같아야 한다.”**
> 대규모 트래픽 환경에서의 **데이터 정합성 보장**과 **시스템 안정성 확보**를 목표로 한 결제 시스템 기술 실험 프로젝트

---

## 1. 프로젝트 개요 (Introduction)

**Mock-Pay**는 상용 결제 서비스의 복잡한 정책 구현보다는,
결제 과정에서 필연적으로 발생하는 **기술적 챌린지**에 집중한 **기술 다이브(Tech Dive) 프로젝트**입니다.

### 해결하고자 한 핵심 질문

* 동시에 수십~수백 건의 결제가 발생할 때 **잔액을 어떻게 안전하게 보호할 것인가?**
* 외부 서비스 장애가 **우리 결제 시스템 전체 중단**으로 이어지지 않게 하려면 어떻게 설계해야 하는가?

### 집중한 기술 주제

* 동시성 제어 (Concurrency Control)
* 멱등성 보장 (Idempotency)
* 장애 격리 및 탄력성 (Resilience)

---

## 2. 기술적 의사결정 (Technical Decisions)

### 🏗 멀티 모듈 아키텍처 (Multi-Module)

**결정**

* `api` : 외부 요청을 처리하는 비즈니스 인터페이스 계층
* `db-core` : 데이터 접근, 도메인, 공통 로직

**이유**

* 핵심 도메인 로직과 인프라 설정의 명확한 분리
* 공통 로직의 재사용성 향상
* 향후 모듈 단위 스케일 아웃 및 **MSA 전환 가능성** 고려

---

### 🔒 Redisson 기반 분산 락 (Distributed Lock)

**결정**

* Lettuce의 스핀 락 방식 대신 **Redisson (Pub/Sub 기반)** 선택

**이유**

* 스핀 락은 락 획득 실패 시 지속적으로 Redis에 요청 → 부하 증가
* Redisson은 락 해제 시점에만 이벤트를 전달
* **대용량 요청 환경에서 Redis 리소스 사용 최소화**

---

### ⚡ Kafka 비동기 이벤트 처리

**결정**

* 포인트 적립 등 부가 로직을 결제 트랜잭션에서 분리하여 Kafka로 처리

**이유**

* 결제 성공 응답 속도 극대화
* 외부 서비스 장애가 메인 결제 트랜잭션 롤백으로 전파되는 것을 방지
* 시스템 간 결합도 감소

---

## 3. 핵심 트러블 슈팅 (Key Trouble Shooting)

### 🚨 Issue 1. 레이스 컨디션 & 비관적 락의 성능 한계

#### Situation

* 150명이 동시에 1,000원씩 결제하는 테스트에서 잔액 불일치 발생
* 테스트 코드로 문제 재현

#### Cause

* 동일 사용자 잔액에 대해 다수 스레드가 동시에 접근
* Read → Modify → Write 과정에서 **Lost Update 발생**

#### Action

1. `PESSIMISTIC_WRITE` 적용

   * 데이터 정합성은 확보
   * DB 커넥션 점유 시간이 길어짐

2. Redis 분산 락으로 전환

   * AOP를 활용하여 락 로직을 비즈니스 코드에서 분리

3. 트랜잭션 경계 문제 해결

   * 트랜잭션 커밋 이전 락 해제 현상 발견
   * `Propagation.REQUIRES_NEW`를 적용한 `AopForTransaction` 클래스 도입

#### Result

* 150건 동시 요청에서도 **잔액 정합성 100% 보장**
* DB 부하 감소 및 응답 안정성 확보

---

### 🚨 Issue 2. Checked Exception 발생 시 롤백 미동작

#### Situation

* 비즈니스 예외 발생 시 잔액 차감이 커밋되는 현상 발견

#### Cause

* Spring `@Transactional` 기본 정책

  * `RuntimeException`만 롤백
  * `Exception` 상속 클래스는 롤백 대상 아님

#### Action

```java
@Transactional(rollbackFor = Exception.class)
```

* 모든 예외 상황에서 트랜잭션 원자성 보장

#### Result

* 예외 유형과 관계없이 데이터 정합성 유지 확인

---

### 🚨 Issue 3. 중복 결제 요청 (Idempotency)

#### Situation

* 네트워크 지연으로 인한 클라이언트 재요청
* 동일 주문에 대한 **중복 결제 위험** 존재

#### Action

* Redis 기반 멱등성 검사기 구현
* `orderId`를 Key로 결제 결과를 24시간 저장
* 동일 ID 요청 시 로직 실행 없이 기존 결과 즉시 반환

#### Result

* 동일 주문에 대해 **잔액 차감은 단 1회만 수행됨을 보장**

---

## 4. 장애 탄력성 (Resilience & Stability)

### 🔁 Circuit Breaker (Resilience4j)

* 외부 은행 서버 장애 시 회로 차단(Open)
* 연쇄 장애(Cascading Failure) 방지
* 주요 설정

  * `failureRateThreshold`: 40%
  * Half-Open 상태를 통한 자동 복구 시나리오 구성

---

### 📦 Kafka Dead Letter Queue (DLQ)

* 비동기 이벤트 처리 중 최종 실패 메시지를 `.DLT` 토픽으로 전송
* 데이터 유실 방지
* 추후 수동 재처리 가능 구조

---

## 5. 한계점 및 회고 (Retrospective)

### 한계점

* 단일 Kafka 브로커 환경으로 **고가용성(HA) 테스트 부족**
* Redis 분산 락 적용 시 Redis 장애에 대한 대비 미흡

  * Redlock 알고리즘 미적용

### 향후 계획

* Prometheus + Grafana 연동

  * Circuit Breaker 상태
  * Kafka Consumer Lag 모니터링
* nGrinder를 활용한 대용량 트래픽 테스트

  * 임계치(Threshold) 정량적 측정

---

## 6. 사용 기술 스택 (Tech Stack)

| Category  | Technology             |
| --------- | ---------------------- |
| Language  | Java 21                |
| Framework | Spring Boot 3.3.4, JPA |
| Storage   | MySQL 8.0, Redis 7.2   |
| Messaging | Apache Kafka 7.5.0     |
| Stability | Resilience4j 2.2.0     |
| Infra     | Docker Compose         |
