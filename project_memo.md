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