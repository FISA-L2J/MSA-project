package com.msa.account_service.dto;

import com.msa.account_service.domain.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionProcessResponse {
	private Long recordId;
	private Long transactionId;
	private BigDecimal newBalance;
	private Status status;
	private LocalDateTime createdAt;
}
