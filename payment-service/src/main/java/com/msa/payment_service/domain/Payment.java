package com.msa.payment_service.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;
    private Long userId;
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
    private LocalDateTime createdAt;

    @Builder
    public Payment(Long orderId, Long userId, BigDecimal amount, PaymentMethod paymentMethod, PaymentStatus status,
            LocalDateTime createdAt) {
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static Payment createPayment(Long orderId, Long userId, BigDecimal amount, PaymentMethod paymentMethod) {
        return Payment.builder()
                .orderId(orderId)
                .userId(userId)
                .amount(amount)
                .paymentMethod(paymentMethod)
                .status(PaymentStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
