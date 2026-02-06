package com.zrp.mockpay.dbcore.repository;

import com.zrp.mockpay.dbcore.entity.PaymentHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, Long> {
    // 지금은 기본 기능만 있으면 됩니다.
}