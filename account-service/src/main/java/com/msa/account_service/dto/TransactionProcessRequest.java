package com.msa.account_service.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TransactionProcessRequest {
	private String userId;
	private BigDecimal amount;
}
