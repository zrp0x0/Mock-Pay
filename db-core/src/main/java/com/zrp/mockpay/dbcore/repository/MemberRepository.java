package com.zrp.mockpay.dbcore.repository;

import com.zrp.mockpay.dbcore.entity.Member;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// ⚠️ interface 입니다! class 아니에요!
public interface MemberRepository extends JpaRepository<Member, Long> {
    // 텅 비어있어도 됩니다.
    // JpaRepository를 상속받는 순간, save(), findById(), findAll() 같은 기능을 공짜로 얻습니다.

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m where m.id = :id")
    Optional<Member> findByIdForUpdate(@Param("id") Long id);
}