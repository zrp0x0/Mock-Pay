package com.zrp.mockpay.dbcore.repository;

import com.zrp.mockpay.dbcore.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

// ⚠️ interface 입니다! class 아니에요!
public interface MemberRepository extends JpaRepository<Member, Long> {
    // 텅 비어있어도 됩니다.
    // JpaRepository를 상속받는 순간, save(), findById(), findAll() 같은 기능을 공짜로 얻습니다.
}