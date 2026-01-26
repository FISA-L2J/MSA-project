package com.msa.order_service.client;

import com.msa.order_service.dto.PaymentRequest;
import com.msa.order_service.dto.PaymentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "payment-service", url = "${payment.service.url:http://localhost:8081}")
public interface PaymentClient {

    @PostMapping("/payment/process")
    PaymentResponse processPayment(@RequestBody PaymentRequest request);
}
