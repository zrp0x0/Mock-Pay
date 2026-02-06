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