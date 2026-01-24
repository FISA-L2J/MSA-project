package com.msa.order_service.dto;

import com.msa.order_service.domain.PaymentMethod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderRequest {
    @NotNull(message = "userId must not be null")
    private Long userId;

    @NotNull(message = "productId must not be null")
    private Long productId;

    @NotNull(message = "productName must not be null")
    private String productName;

    @Min(value = 1, message = "quantity must be at least 1")
    private Integer quantity;

    @Min(value = 0, message = "unitPrice must be non-negative")
    private BigDecimal unitPrice;

    @NotNull(message = "paymentMethod must not be null")
    private PaymentMethod paymentMethod;
}
