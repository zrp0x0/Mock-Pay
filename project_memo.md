# Mock-Pay 
 
---

## Day 001

### 프로젝트 뼈대 "멀티 모듈" 이해하기
- 프로젝트를 시작하기 전에 멀티 모듈이라는 구조를 잡아보자
    - Core 모듈: 모든 요리에 공통으로 들어가는 육수나 양념 (공통 코드)
    - API 모듈: 손님에게 요리를 내어주는 서빙 공간 (사용자 인터페이스)
    - Batch 모듈: 마감 후 설거지나 정산을 하는 뒷공간 공간 (배치 작업)

- 이렇게 하는 이유
    - 나중에 특정 모율에 문제가 생겨고 다른 모듈에는 영향을 주지 않고, 필요한 부분만 고쳐쓸 수 있음

### 빈 프로젝트 생성하기
- Name: mock-pay
- Language: Java 21
- Build System: Gradle-Kotlin
- Dependency: None

### 본사(Root)와 지사(Module) 나누기
- 현재 mock-pay는 본사(Head Office) 역할을 해야함
- 본사는 직접 코딩을 하지 않고, 지사들을 관리만 함
- 지사 목록을 만들어야함

```java
// settings.gradle.kts
rootProject.name = "mock-pay"

// 두 개의 지사(모듈)
include("db-core")  // 데이터베이스와 관련된 핵심 로직 (육수 공장)
include("api")      // 사용자 요청을 받는 서버 (레스토랑 매장)
```

---

## Day 001

### 본사의 역할 변경
- 현재 build.gradle.kts는 혼자 다 할거야라고 적혀있음
- 나는 관리만하고 일은 지사들이 한다로 바꿔줘야함

### 폴더 구조 잡기 & 본사 규칙 설정
- 폴더 만들기 (root dir 바로 아래)
    - api/
    - db-core/

- 빈 설정 파일 만들기
    - api/build.gradle.kts (빈 파일)
    - db-core/build.gradle.kts (빈 파일)

- 본사 build.gradle.kts 전면 수정
    ```java
    plugins {
        java
        // 스프링 부트 플러그인 (버전 관리 매니저)
        id("org.springframework.boot") version "3.3.4"
        id("io.spring.dependency-management") version "1.1.6"
    }

    group = "com.zrp"
    version = "0.0.1-SNAPSHOT"

    // 본사(Root)는 실행 가능한 파일(Jar)을 만들 필요가 없습니다. 관리만 하니까요!
    tasks.bootJar { enabled = false }
    tasks.jar { enabled = false }

    // subprojects: "모든 지사(api, db-core)들에게 이 규칙을 적용한다"
    subprojects {
        apply(plugin = "java")
        apply(plugin = "org.springframework.boot")
        apply(plugin = "io.spring.dependency-management")

        java {
            toolchain {
                languageVersion = JavaLanguageVersion.of(21)
            }
        }

        repositories {
            mavenCentral()
        }

        // 모든 지사가 기본적으로 가질 공통 의존성
        dependencies {
            implementation("org.springframework.boot:spring-boot-starter")
            testImplementation("org.springframework.boot:spring-boot-starter-test")
            testRuntimeOnly("org.junit.platform:junit-platform-launcher")
        }

        tasks.withType<Test> {
            useJUnitPlatform()
        }
    }
    ```

- 본사 src 폴더 삭제
    - 이제 각 모듈별로 src 폴더를 만들 것임

### 각 지사 api, db-core가 실제로 일을 할 수 있도록 작업 공간을 만들어줘야함
- API 모듈 설정
    - api/src/main/java/com/zrp/mockpay/api/ApiApplication.java
    ```java
    package com.zrp.mockpay.api;

    import org.springframework.boot.SpringApplication;
    import org.springframework.boot.autoconfigure.SpringBootApplication;

    @SpringBootApplication
    public class ApiApplication {
        public static void main(String[] args) {
            SpringApplication.run(ApiApplication.class, args);
        }
    }
    ```

- DB-Core 모듈 설정
    - db-core/src/main/java/com/zrp/mockpay/dbcore/

### vscode의 패키지를 못잡는 버그?
- 패키지 명으로 인한 오류는 신경쓰지 말 것
- run으로 실행시키지 말 것
- terminal: **.\gradlew clean build**

### API 모듈에 dependencies 설정을 해서 웹 서버 띄우기
- API build.gradle.kts
    ```java
    dependencies {
        // 본사(Root)에서 가져온 기본 재료에 더해서...
        
        // 이 한 줄이 "톰캣(Tomcat)"이라는 웹 서버를 내장하고 있습니다.
        implementation("org.springframework.boot:spring-boot-starter-web")
    }
    ```

### API 모듈에 Controller 작성
- 이제 API 요청을 받을 Controller를 작성해야함
    ```java
    package com.zrp.mockpay.api.controller;

    import org.springframework.web.bind.annotation.GetMapping;
    import org.springframework.web.bind.annotation.RestController;

    @RestController
    public class HealthCheckController {

        // 누군가 "http://localhost:8080/health" 로 접속하면 이 메서드가 실행됩니다.
        @GetMapping("/health")
        public String healthCheck() {
            return "Mock-Pay API Server is Running! (OK)";
        }
    }
    ```

### 데이터 센터 구축하기 (Docker)
- 현재 API는 있는데, 장부(DB)를 보관할 창고가 없음
- 데이터베이스(MySQL) / 캐시 메모리(Redis)을 사용

- Docker-Compose.yml
    ```yaml
    version: '3.8'

    services:
    # 1. 우리의 메인 데이터베이스 (장부)
    mock-mysql:
        image: mysql:8.0
        container_name: mock-mysql
        ports:
        - "3307:3306"
        environment:
        MYSQL_ROOT_PASSWORD: root
        MYSQL_DATABASE: mockpay
        command: 
        # 한글 깨짐 방지 설정
        - --character-set-server=utf8mb4
        - --collation-server=utf8mb4_unicode_ci

    # 2. 아주 빠른 임시 저장소 (포스트잇)
    mock-redis:
        image: redis:7.2
        container_name: mock-redis
        ports:
        - "6379:6379"
    ```

    - "3307:3306": 나의 컴퓨터 3307 포트로 들어오면 컨테이너 내부의 MySQL 3306포트로 보내줘라는 의미 (내가 지금 MySQL을 로컬로 설치해서 3306 포트를 사용하고 있음)

    - 실행: docker-compose up -d

### 애플리케이션과 DB 연결하기
- db-core의 build.gradle.kts 설정
    ```java
    dependencies {
        // 1. 스프링 데이터 JPA (자바 코드로 쿼리를 짜게 해주는 마법 도구)
        implementation("org.springframework.boot:spring-boot-starter-data-jpa")
        
        // 2. MySQL 드라이버 (자바와 MySQL이 대화하기 위한 통역사)
        runtimeOnly("com.mysql:mysql-connector-j")
    }
    ```

- API - DB-Core 연결하기
    - api의 build.gradle.kts에 db-core의 기능을 가져다 쓴다고 해야함
    ```java
    dependencies {
        implementation("org.springframework.boot:spring-boot-starter-web")
        
        // 👇 [추가] "우리 매장은 db-core 공장에서 만든 기능을 갖다 씁니다."
        implementation(project(":db-core"))
    }
    ```

- 접속 정보 입력 (application.yml)
    - api/src/main/resources/application.yml
    ```yml
    spring:
        datasource:
            # 아까 우리가 3307 포트로 바꿨었죠? 여기서 그 주소를 씁니다.
            url: jdbc:mysql://localhost:3307/mockpay
            username: root
            password: root
            driver-class-name: com.mysql.cj.jdbc.Driver

        jpa:
            hibernate:
            # 테이블을 자동으로 만들어주는 옵션 (실무에선 validate를 쓰지만 학습용은 update)
            ddl-auto: update
            properties:
            hibernate:
                format_sql: true  # 실행되는 SQL을 예쁘게 보여줘
                
        logging:
        level:
            org.hibernate.SQL: debug # 실행되는 쿼리를 로그에 찍어줘
    ```

### 자바 코드로 테이블 만들기 (JPA Entity)
- db-core에 entity 폴더를 만들고, Member.java 만들기
    ```java
    package com.zrp.mockpay.dbcore.entity;

    import jakarta.persistence.*;

    @Entity // "이 클래스는 DB 테이블이 될 거야!" 라고 선언
    @Table(name = "member") // 테이블 이름 지정
    public class Member {

        @Id // "이게 주민등록번호 같은 고유 키(PK)야"
        @GeneratedValue(strategy = GenerationType.IDENTITY) // "번호는 1씩 자동으로 증가시켜 줘 (Auto Increment)"
        private Long id;

        @Column(nullable = false, length = 50) // "반드시 값이 있어야 하고(Not Null), 50자 제한이야"
        private String name;

        @Column(nullable = false, unique = true) // "이메일은 중복되면 안 돼"
        private String email;

        // JPA는 기본 생성자가 필수입니다 (보호된 수준으로)
        protected Member() {
        }

        public Member(String name, String email) {
            this.name = name;
            this.email = email;
        }
        
        // Getter (값 확인용)
        public Long getId() { return id; }
        public String getName() { return name; }
        public String getEmail() { return email; }
    }
    ```

- 이제 스캔 범위를 com.zrp.mockpay로 만들어서 시야를 넓혀줘야함
    - @SpringBootApplication(scanBasePackages = "com.zrp.mockpay")
    - @EntityScan("com.zrp.mockpay"): 이 동네에 있는 Entity도 다 찾아보라고 해야함

- Docker 명령어로 테이블이 생성되었는지 보기
    - docker exec -it mock-mysql mysql -u root -proot -D mockpay -e "SHOW TABLES;"

### 창고 관리자 채용하기 (Repository)
- db-core에 Repository 폴더를 만들고 memberRepository.java 만들기
    ```java
    package com.zrp.mockpay.dbcore.repository;

    import com.zrp.mockpay.dbcore.entity.Member;
    import org.springframework.data.jpa.repository.JpaRepository;

    // ⚠️ interface 입니다! class 아니에요!
    public interface MemberRepository extends JpaRepository<Member, Long> {
        // 텅 비어있어도 됩니다.
        // JpaRepository를 상속받는 순간, save(), findById(), findAll() 같은 기능을 공짜로 얻습니다.
    }
    ```

- Controller 수정
    ```java
    package com.zrp.mockpay.api.controller;

    import com.zrp.mockpay.dbcore.entity.Member;
    import com.zrp.mockpay.dbcore.repository.MemberRepository;
    import org.springframework.web.bind.annotation.GetMapping;
    import org.springframework.web.bind.annotation.RestController;

    @RestController
    public class HealthCheckController {

        private final MemberRepository memberRepository;

        // 생성자 주입: "스프링아, 창고 관리자(Repository) 좀 데려와줘"
        public HealthCheckController(MemberRepository memberRepository) {
            this.memberRepository = memberRepository;
        }

        @GetMapping("/health")
        public String healthCheck() {
            // 1. 새 멤버 생성 (이름은 랜덤하게 시간으로)
            String name = "User-" + System.currentTimeMillis();
            String email = name + "@test.com";
            Member newMember = new Member(name, email);

            // 2. DB에 저장 (save 메서드는 우리가 안 만들었지만 공짜로 씁니다)
            memberRepository.save(newMember);

            return "OK! 새로운 멤버가 저장되었습니다: " + name;
        }
    }
    ```

### 특정 모듈의 implementation을 가져다 쓰기
- db-core에만 JpaRepository 의존성이 있기 때문에
- api의 Controller에서 사용할 수 없음
- db-core의 build.gradle.kts 재설정
```kotlin
plugins {
    // 1. "api" 라는 단어를 쓰기 위해 필요한 플러그인입니다.
    `java-library`
}

dependencies {
    // 2. implementation -> api 로 변경!
    // "나를 가져다 쓰는 애들(api 모듈)한테도 이 라이브러리를 공개할게!"
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    
    // MySQL 드라이버는 실행할 때만 필요하니 그대로 둡니다.
    runtimeOnly("com.mysql:mysql-connector-j")
}
```

### 스캔 범위 재설정
- JpaRepository의 스캔 범위도 넓혀줘야함
```java
package com.zrp.mockpay.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories; // 👈 import 필수!

@SpringBootApplication(scanBasePackages = "com.zrp.mockpay")
@EntityScan("com.zrp.mockpay")
@EnableJpaRepositories("com.zrp.mockpay") // 👈 [추가] "이 동네에 있는 리포지토리(관리자)들도 다 채용해!"
public class ApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }
}
```

- 이제 실행 후, 도커 명령어로 데이터가 실제로 저장이되는지 확인
    - docker exec -it mock-mysql mysql -u root -proot -D mockpay -e "SELECT * FROM member;"

### Redis 사용이유
- MySQL: 하드디스크: 쉽게 말하자면 데이터를 찾으러 갔다오는데 오랜 시간이 걸림
- Redis: 메모리: 쉽게 말하면 데이터베이스보다 더 빠르게 데이터를 가져올 수 있음 (그 외 추가 기능들이 더 있긴한데 캐시 메모리로 자주 사용함)
    - 참고로 레디스는 텍스트로 저장을 함

### Redis 의존성 설정
- db-core의 build.gradle.kts 의존성 재설정
```kotlin
dependencies {
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    
    // 👇 [추가] "Redis 도구함 추가!"
    api("org.springframework.boot:spring-boot-starter-data-redis") 
    
    runtimeOnly("com.mysql:mysql-connector-j")
}
```

- application.yml로 Redis 주소를 적어서 알려줘야함
```yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3307/mockpay
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        format_sql: true
        
  # 👇 [추가] Redis 설정 (spring 밑에 줄을 잘 맞춰주세요!)
  data:
    redis:
      host: localhost
      port: 6379

logging:
  level:
    org.hibernate.SQL: debug
```

- Controller 수정
```java
package com.zrp.mockpay.api.controller;

import com.zrp.mockpay.dbcore.entity.Member;
import com.zrp.mockpay.dbcore.repository.MemberRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

    private final MemberRepository memberRepository;
    private final StringRedisTemplate redisTemplate; // 👈 Redis를 다루는 도구

    // 생성자 주입: "MySQL 담당자랑 Redis 담당자 둘 다 데려와!"
    public HealthCheckController(MemberRepository memberRepository, StringRedisTemplate redisTemplate) {
        this.memberRepository = memberRepository;
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/health")
    public String healthCheck() {
        // 1. MySQL 저장
        String name = "User-" + System.currentTimeMillis();
        Member newMember = new Member(name, name + "@test.com");
        memberRepository.save(newMember);

        // 2. Redis 저장 (Key: "status", Value: "Health Check OK")
        // Redis는 복잡한 객체보다 단순한 문자열(String)을 주로 저장합니다.
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        ops.set("currentStatus", "Server is Running! (" + name + ")");

        // 3. Redis에서 다시 꺼내보기
        String redisValue = ops.get("currentStatus");

        return "DB ID: " + newMember.getId() + " / Redis Value: " + redisValue;
    }
}
```

- docker 명령어로 redis 컨테이너에 접근해서 데이터 확인
    - docker exec -it mock-redis redis-cli get currentStatus
    - currentStatus라는 키로 저장을 했음
    - 그 키에 해당하는 값을 가져와서 보여달라는 의미

### 지금까지 내용 총정리
- 멀티 모듈로 프로젝트 분리
- Docker Compose를 사용해서 MySQL / Redis를 띄우기
- Spring Data JPA 설정
- Spring Data Redis 설정

### 추가적으로 보완할 내용
- redis key 전략
    - health:current
    - member:{id}
    - payment:{orderId}

- docker 띄우기
    - 실행: docker-compose up -d
    - 종료: docker-compose down
    - 캐시 지우고 강한 실행: .\gradlew clean :api:bootRun

---

## Day 002. 내 지갑에 100억 충전하기

### 설계도 그리기 (ERD)
- Member (회원)
    - 이름 / 이메일 / balance(잔액)

- PaymentHistory (거래 내역)
    - 누가(Member), 얼마를(amount), 충전했는지/썼는지(type), 언제(createAt)
    - 관게: 한 명의 회원은 여러 개의 내역을 가질 수 있습니다. (1 : N 관계)

### Member Entity 업데이트
```java
package com.zrp.mockpay.dbcore.entity;

import jakarta.persistence.*;

@Entity // "이 클래스는 DB 테이블이 될 거야!" 라고 선언
@Table(name = "member") // 테이블 이름 지정
public class Member {

    @Id // "이게 주민등록번호 같은 고유 키(PK)야"
    @GeneratedValue(strategy = GenerationType.IDENTITY) // "번호는 1씩 자동으로 증가시켜 줘 (Auto Increment)"
    private Long id;

    @Column(nullable = false, length = 50) // "반드시 값이 있어야 하고(Not Null), 50자 제한이야"
    private String name;

    @Column(nullable = false, unique = true) // "이메일은 중복되면 안 돼"
    private String email;

    @Column(nullable = false)
    private Long balance = 0L; // 잔액 필드 (기본값 0원)

    // JPA는 기본 생성자가 필수입니다 (보호된 수준으로)
    protected Member() {
    }

    public Member(String name, String email) {
        this.name = name;
        this.email = email;
    }

    // 잔액 충전 (비즈니스 로직 - 돈 관리 기능)
    public void charge(Long amount) {
        this.balance += amount;
    }

    // 잔액 사용
    public void use(Long amount) {
        if (this.balance < amount) {
            throw new IllegalArgumentException("잔액이 부족합니다.");
        }
        this.balance -= amount;
    }
    
    // Getter (값 확인용)
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public Long getBalance() { return balance; } // 잔액 확인용
}
```
- 추가 조언: setBalance(100)처럼 값을 덮어쓰는 Setter를 만드는 것보다 의미 있는 메서드를 Entity 안에 만드는 것이 좋음 (DDD 흉내)

### 거래 유형 (Enum) 만들기
```java
package com.zrp.mockpay.dbcore.enums;

public enum PaymentType {
    CHARGE, // 충전
    USE     // 사용 (결제)
}
```

### PaymentHistory Entity 만들기
- 영주증 역할을 할 엔티티를 만들자
```java
package com.zrp.mockpay.dbcore.entity;

import com.zrp.mockpay.dbcore.enums.PaymentType;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_history")
@EntityListeners(AuditingEntityListener.class) // 👇 자동으로 시간을 기록해주는 기능 활성화
public class PaymentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 👇 중요: Member와 연결 (N:1 관계)
    // "이 영수증의 주인은 누구인가?"를 저장합니다.
    @ManyToOne(fetch = FetchType.LAZY) 
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private Long amount; // 거래 금액

    @Enumerated(EnumType.STRING) // DB에 숫자가 아닌 "CHARGE", "USE" 글자로 저장됨
    @Column(nullable = false)
    private PaymentType paymentType;

    @CreatedDate // 데이터가 생성될 때 자동으로 현재 시간이 들어감
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected PaymentHistory() {
    }

    public PaymentHistory(Member member, Long amount, PaymentType paymentType) {
        // 👇 [추가] 생성자에서 null 체크 (방어 로직)
        if (member == null) {
            throw new IllegalArgumentException("거래 내역 생성 시 회원 정보는 필수입니다.");
        }
        if (amount == null || amount < 0) { // 금액도 음수면 안되겠죠?
            throw new IllegalArgumentException("거래 금액이 올바르지 않습니다.");
        }

        this.member = member;
        this.amount = amount;
        this.paymentType = paymentType;
    }

    // Getter
    public Long getId() { return id; }
    public Member getMember() { return member; }
    public Long getAmount() { return amount; }
    public PaymentType getPaymentType() { return paymentType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
```

### PaymentHistoryRepository 만들기
```java
package com.zrp.mockpay.dbcore.repository;

import com.zrp.mockpay.dbcore.entity.PaymentHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, Long> {
    // 지금은 기본 기능만 있으면 됩니다.
}
```

### Auditing 활성화
- 여기다가하면 나중에 Test때 문제가 발생할 수 있지만, 그건 나중에 필요하면 하자
    - 원래는 JpaConfig.java를 하나 만들고 거기에서 붙여서 실행하는 경우도 있음 (Toy_Project_01 때 해봤잖아)
```java
// ... import 생략 ...
import org.springframework.data.jpa.repository.config.EnableJpaAuditing; // 추가

@SpringBootApplication(scanBasePackages = "com.zrp.mockpay")
@EntityScan("com.zrp.mockpay")
@EnableJpaRepositories("com.zrp.mockpay")
@EnableJpaAuditing // 👈 [추가] "자동으로 시간 기록하는 기능(Auditing)을 켜라!"
public class ApiApplication {
    // ... main ...
}
```

### DB 반영 정보 확인
- Docker 명령어
    - docker exec -it mock-mysql mysql -u root -proot -D mockpay -e "DESC member; DESC payment_history;"
    - 테이블 설정 정보를 보여주는 명령어

- DB 수정을 하려면?
    - update로 설정되어있는데 DB의 컬럼 설정이 바뀌면 DB를 완전히 다시 생성해야함
    - create로 하는 방법도 있고
    - docker exec -it mock-mysql mysql -u root -proot -D mockpay 명령어를 입력한 후
    - mysql> DROP TABLE payment_history; 로 테이블을 제거한 후, 다시 서버를 실행하면 됨

### DTO와 Service(비즈니스) 로직 분리
- DTO를 사용하는 이유(Data Transfer Object)
    - Entity의 내용은 외부에 노출되면 안됨
    - 필요한 내용만 받고 Entity로 변환 후 DB에 저장하는 방식을 사용하는 것이 안전함
    - 보통 Controller <-> Service는 DTO로 데이터를 주고 받고,
    - Service <-> DAO(Repository)는 Entity로 데이터를 주고 받음

- DTO는 보통 (_Response / _Request)로 나뉨

- 그리고 현재 Controller에서 Repository를 직접 호출하는 방식인데 이는 좋지 않음
- Controller는 API의 요청만 받고 + 전처리할 게 있으면 하는 역할 정도로 분리
- 실제 비즈니스 로직은 Servie에서 처리하는 것이 좋음 (나중에 테스트도 분리할 수 있음)

- 보통 Layered Architecture
    - MVC를 뜻하는 건 아님 (물론 이것도 Layered Architecture)
    - 여기서 말하는 건
        - Controller <-> Service <-> Repository <-> DB

### DTO 만들기
- PaymentRequest.java
```java
package com.zrp.mockpay.api.dto;

// record: "데이터만 담는 그릇"을 만드는 최신 문법 (Getter, 생성자 자동 생성)
public record PaymentRequest(
    Long memberId,
    Long amount
) {}
```

- PaymentResponse.java
```java
package com.zrp.mockpay.api.dto;

public record PaymentResponse(
    String result,  // "성공", "실패" 메시지
    Long balance    // 거래 후 잔액
) {}
```

### Service 로직 만들기
```java
package com.zrp.mockpay.api.service;

import com.zrp.mockpay.api.dto.PaymentRequest;
import com.zrp.mockpay.api.dto.PaymentResponse;
import com.zrp.mockpay.dbcore.entity.Member;
import com.zrp.mockpay.dbcore.entity.PaymentHistory;
import com.zrp.mockpay.dbcore.enums.PaymentType;
import com.zrp.mockpay.dbcore.repository.MemberRepository;
import com.zrp.mockpay.dbcore.repository.PaymentHistoryRepository;
import jakarta.transaction.Transactional; // ⚠️ org.springframework... 가 아니라 jakarta... 를 써도 되지만, 보통 스프링에선 org.springframework.transaction.annotation.Transactional을 씁니다. (아래 설명 참조)
import org.springframework.stereotype.Service;

@Service // "나는 비즈니스 로직을 담당하는 직원(Service)이야"
public class PaymentService {

    private final MemberRepository memberRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;

    public PaymentService(MemberRepository memberRepository, PaymentHistoryRepository paymentHistoryRepository) {
        this.memberRepository = memberRepository;
        this.paymentHistoryRepository = paymentHistoryRepository;
    }

    // 👇 [중요] Transactional: 이 메서드가 끝날 때까지 에러가 없어야 DB에 반영됨!
    // 하나라도 실패하면 없던 일로 되돌림 (Rollback)
    @org.springframework.transaction.annotation.Transactional
    public PaymentResponse charge(PaymentRequest request) {
        // 1. 손님 찾기 (없으면 에러)
        Member member = memberRepository.findById(request.memberId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 2. 잔액 충전 (Member 엔티티의 비즈니스 로직 사용)
        member.charge(request.amount());

        // 3. 영수증 기록
        PaymentHistory history = new PaymentHistory(member, request.amount(), PaymentType.CHARGE);
        paymentHistoryRepository.save(history);

        // 4. 결과 리턴
        return new PaymentResponse("충전 성공", member.getBalance());
    }

    @org.springframework.transaction.annotation.Transactional
    public PaymentResponse use(PaymentRequest request) {
        // 1. 손님 찾기
        Member member = memberRepository.findById(request.memberId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 2. 잔액 사용 (잔액 부족하면 여기서 에러 터짐 -> 자동 롤백)
        member.use(request.amount());

        // 3. 영수증 기록
        PaymentHistory history = new PaymentHistory(member, request.amount(), PaymentType.USE);
        paymentHistoryRepository.save(history);

        // 4. 결과 리턴
        return new PaymentResponse("결제 성공", member.getBalance());
    }
}
```
- 여기서 잠깐, Transactional이란?
    - 데이터 정합성과 무결성을 보장하기 위해 사용
    - 간단하게 말하자면, 데이터의 처리가 4개가 있다면, 4개 중 1개만 실패하더라도 초기 상태로 롤백하여서 데이터에 문제가 발생하지 않도록 하는 영역이라는 뜻

- 데이터 정합성:
    - 트랜잭션 전후에 데이터베이스 상태가 모순이 없이 일관되어야 함

- 데이터 무결성:
    - 데이터가 오류 없이 정확하고 유효한 상태를 유지함을 보장함

- 트랜잭션 핵심 원칙:
    - ACID: Atomicity / Consistency / Isolatin / Durability

### Controller
```java
package com.zrp.mockpay.api.controller;

import com.zrp.mockpay.api.dto.PaymentRequest;
import com.zrp.mockpay.api.dto.PaymentResponse;
import com.zrp.mockpay.api.service.PaymentService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment") // 이 컨트롤러의 공통 주소
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // 충전 API: POST /api/payment/charge
    @PostMapping("/charge")
    public PaymentResponse charge(@RequestBody PaymentRequest request) {
        return paymentService.charge(request);
    }

    // 결제 API: POST /api/payment/use
    @PostMapping("/use")
    public PaymentResponse use(@RequestBody PaymentRequest request) {
        return paymentService.use(request);
    }
}
```

### 테스트 진행
- 현재 UI(View)가 없으므로 Postman을 사용하여 Post 테스트 진행
- 회원 생성은 일단 간단하게 healthcheckcontroller로 생성하여 user의 id로 진행
```text
### 1. 헬스 체크 (서버 켜졌나 확인 + 멤버 생성)
GET http://localhost:8080/health

### 2. 잔액 충전 (10,000원) - 위에서 만든 멤버 ID를 확인하고 고쳐주세요! (보통 1번)
POST http://localhost:8080/api/payment/charge
Content-Type: application/json

{
  "memberId": 1770359835189,
  "amount": 10000
}

### 3. 잔액 사용 (5,000원)
POST http://localhost:8080/api/payment/use
Content-Type: application/json

{
  "memberId": 1770359835189,
  "amount": 5000
}
```

### 마지막 DB 실제 데이터 확인
- docker 명령어 
    -docker exec -it mock-mysql mysql -u root -proot -D mockpay -e "SELECT * FROM payment_history; SELECT * FROM member;"

### 추가적으로 보완한 내용
- PaymentHistory
    - 잔액(balance)를 믿으면 안됨
    - **잔액은 결과일 뿐, 진실은 로그에 있음**
    - 거래의 내역을 통해서 잔액을 보장함 (학부 때 블록체인도, 기록으로 거래를 인증하는 개념을 배움(해싱))

- 방어적 프로그래밍
    - 돈과 관련된 로직에서 연산하는 부분이 가장 중요함
    - 나중에 로그를 기반으로 잔액을 검증하는 시스템도 구축해볼 수 있겠지만 일단은 이번 프로젝트의 학습 목표가 아닌 것 같음
    
### 프로젝트 방향성 리마인드
- 일단 모듈을 분리해서 개발을 해보고 싶었음 (유튜브에서 봤는데, MSA 이런 느낌이 아니였음)
- 또한 결제 로직에만 집중해서 개발을 진행해보고 싶음 (Toy_Project_01에서 DB락, 대기열 시스템을 공부해봤는데, 개념을 어느정도 이해한 것 + 만들어보기 + 이런게 있구나 알기(정확한 이해X))
- 물론 이 프로젝트에서 docker-compose / gradle-kotlin을 써보는 것도 처음이지만 이 부분은 개념적인건 알고 있음 (일단 이정도만 필요한 듯 환경 세팅 때문에)
- 그리고 현재 공부 방법은 예전에 코드 구현을 어느정도 해보았기 때문에 AI의 코드를 검증하는 과정을 위주로 하고 있음
- 그래도 모르는 라이브러리 사용법 / 도구 사용법은 계속 경험적으로 체득하려고 하고 있음

### 이후로 계속 해볼 것 (Day 003~)
- Day 003-1. 결제 중복 방지
    - Redis로 결제 요청 ID 저장
    - 같은 요청 2번 오면 차단
    - 실무에서 진짜 많이 나옴

- Day 003-2. 동시성 제어
    - 잔액 차감 race condition 해결
    - @Version 써보기
    - “왜 테스트에서만 터지는지” 이해됨

- Day 003-3. 거래 내역 조회 API
    - 회원별 결제 내역 조회
    - 페이징
    - 최신순 정렬

- Day 003-4. 테스트
    - PaymentService 단위 테스트
    - H2 vs Testcontainers
    - 롤백 테스트