dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")

    // 👇 [추가] "우리 매장은 db-core 공장에서 만든 기능을 갖다 씁니다."
    implementation(project(":db-core"))

    // 👇 [추가] Kafka를 쓰기 위한 필수 라이브러리
    implementation("org.springframework.kafka:spring-kafka")

    // 👇 이걸로 넣어주세요! (Resilience4j Spring Boot 3 Starter)
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
    implementation("org.springframework.boot:spring-boot-starter-aop")
}