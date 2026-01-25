package com.msa.payment_service.service;

import com.msa.payment_service.domain.Payment;
import com.msa.payment_service.dto.PaymentRequest;
import com.msa.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import com.msa.payment_service.domain.PaymentStatus;

import com.msa.payment_service.dto.PaymentResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("Processing payment for orderId: {}", request.getOrderId());

        Payment payment = Payment.createPayment(
                request.getOrderId(),
                request.getUserId(),
                request.getAmount(),
                request.getPaymentMethod());

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment processed successfully with ID: {}", savedPayment.getId());

        return PaymentResponse.builder()
                .paymentId(savedPayment.getId())
                .status(savedPayment.getStatus())
                .amount(savedPayment.getAmount())
                .orderId(savedPayment.getOrderId())
                .createdAt(savedPayment.getCreatedAt())
                .build();
    }
}
