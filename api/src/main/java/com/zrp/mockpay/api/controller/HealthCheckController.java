package com.zrp.mockpay.api.controller;

import com.zrp.mockpay.dbcore.entity.Member;
import com.zrp.mockpay.dbcore.repository.MemberRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@RestController
public class HealthCheckController {

    private final MemberRepository memberRepository;
    private final StringRedisTemplate redisTemplate; // 👈 Redis를 다루는 도구

    // 생성자 주입: "스프링아, 창고 관리자(Repository) 좀 데려와줘"
    public HealthCheckController(MemberRepository memberRepository, StringRedisTemplate redisTemplate) {
        this.memberRepository = memberRepository;
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/health")
    public String healthCheck() {
        // 1. 새 멤버 생성 (이름은 랜덤하게 시간으로)
        String name = "User-" + System.currentTimeMillis();
        String email = name + "@test.com";
        Member newMember = new Member(name, email);

        // 2. DB에 저장 (save 메서드는 우리가 안 만들었지만 공짜로 씁니다)
        memberRepository.save(newMember);

        System.out.println("OK! 새로운 멤버가 저장되었습니다: " + name);

        // 2. Redis 저장 (Key: "status", Value: "Health Check OK")
        // Redis는 복잡한 객체보다 단순한 문자열(String)을 주로 저장합니다.
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        ops.set("currentStatus", "Server is Running! (" + name + ")");

        // 3. Redis에서 다시 꺼내보기
        String redisValue = ops.get("currentStatus");

        return "DB ID: " + newMember.getId() + " / Redis Value: " + redisValue;
    }
}