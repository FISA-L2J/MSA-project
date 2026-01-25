package com.msa.payment_service.dto;

import com.msa.payment_service.domain.PaymentMethod;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PaymentRequest {
    private Long orderId;
    private Long userId;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
}
