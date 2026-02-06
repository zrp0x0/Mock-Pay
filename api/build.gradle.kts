dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")

    // 👇 [추가] "우리 매장은 db-core 공장에서 만든 기능을 갖다 씁니다."
    implementation(project(":db-core"))
}