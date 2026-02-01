package com.msa.account_service.dto;

import com.msa.account_service.domain.Status;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class WithdrawalResponse {
	private Long transactionId;
	private String userId;
	private BigDecimal amount;
	private BigDecimal newBalance;
	private Status status;
	private LocalDateTime createdAt;
}
