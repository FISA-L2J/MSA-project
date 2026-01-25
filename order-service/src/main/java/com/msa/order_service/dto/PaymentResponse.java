package com.msa.order_service.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class PaymentResponse {
    private Long paymentId;
    private String status;
    private BigDecimal amount;
    private Long orderId;
    private LocalDateTime createdAt;
}
