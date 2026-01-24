package com.msa.payment_service.dto;

import com.msa.payment_service.domain.PaymentStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {
    private Long paymentId;
    private PaymentStatus status;
    private BigDecimal amount;
    private Long orderId;
    private LocalDateTime createdAt;
}
