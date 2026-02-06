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
