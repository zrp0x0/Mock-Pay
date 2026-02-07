package com.zrp.mockpay.api.controller;

import com.zrp.mockpay.api.dto.PaymentRequest;
import com.zrp.mockpay.api.dto.PaymentResponse;
import com.zrp.mockpay.api.service.PaymentFacade;
import com.zrp.mockpay.api.service.PaymentService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment") // 이 컨트롤러의 공통 주소
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentFacade paymentFacade;

    public PaymentController(PaymentService paymentService, PaymentFacade paymentFacade) {
        this.paymentService = paymentService;
        this.paymentFacade = paymentFacade;
    }

    // 충전 API: POST /api/payment/charge
    @PostMapping("/charge")
    public PaymentResponse charge(@RequestBody PaymentRequest request) {
        return paymentService.charge(request);
    }

    // 결제 API: POST /api/payment/use
    @PostMapping("/use")
    public PaymentResponse use(@RequestBody PaymentRequest request) {
        // return paymentService.use(request);
        return paymentFacade.use(request);
    }
}