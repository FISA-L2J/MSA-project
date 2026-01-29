package com.msa.transaction_service.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionProcessResponse {
	private Long transactionId;
	private BigDecimal newBalance;
	private String status;
	private LocalDateTime createdAt;
}
