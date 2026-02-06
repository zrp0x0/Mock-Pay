plugins {
	java
	id("org.springframework.boot") version "3.3.4"
	id("io.spring.dependency-management") version "1.1.6"
}

group = "com.zrp"
version = "0.0.1-SNAPSHOT"
description = "Demo project for Spring Boot"

// 본사(Root)는 실행 가능한 파일(Jar)을 만들 필요가 없음. 관리만!
tasks.bootJar { enabled = false }
tasks.jar { enabled = false }

// subprojects: 모든 지사(api, db-core)들에게 이 규칙을 적용
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
