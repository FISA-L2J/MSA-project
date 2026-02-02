package com.msa.transaction_service.dto;

import com.msa.transaction_service.domain.Status;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionProcessResponse {
	private Long recordId;
	private Long transactionId;
	private BigDecimal newBalance;
	private Status status;
	private LocalDateTime createdAt;
}
