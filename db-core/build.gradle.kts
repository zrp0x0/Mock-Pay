plugins {
    // 1. "api" 라는 단어를 쓰기 위해 필요한 플러그인입니다.
    `java-library`
}

dependencies {
    // 1. 스프링 데이터 JPA (자바 코드로 쿼리를 짜게 해주는 마법 도구)
    // implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    
    // implementation -> api로 변경
    // 나를 가져다 쓰는 애들(api 모듈)한테도 이 라이브러리를 공개할게!
    api("org.springframework.boot:spring-boot-starter-data-jpa")

    // 2. MySQL 드라이버 (자바와 MySQL이 대화하기 위한 통역사)
    runtimeOnly("com.mysql:mysql-connector-j")

    // Redis 도구함 추가
    api("org.springframework.boot:spring-boot-starter-data-redis")

    // Redis 락을 쉽고 강력하게 쓰게 해주는 도구
    api("org.redisson:redisson-spring-boot-starter:3.34.1")
}