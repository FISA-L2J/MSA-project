package com.msa.transaction_service.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionProcessRequest {
	private Long userId;
	private BigDecimal amount;
}
