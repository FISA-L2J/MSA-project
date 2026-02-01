package com.msa.transaction_service.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionProcessRequest {
	private String userId;
	private BigDecimal amount;
}
