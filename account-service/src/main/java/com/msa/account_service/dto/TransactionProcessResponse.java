package com.msa.account_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionProcessResponse {
	private Long transactionId;
	private BigDecimal newBalance;
	private String status;
	private LocalDateTime createdAt;
}
