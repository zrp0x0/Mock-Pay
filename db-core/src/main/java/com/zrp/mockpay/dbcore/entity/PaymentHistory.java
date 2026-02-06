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