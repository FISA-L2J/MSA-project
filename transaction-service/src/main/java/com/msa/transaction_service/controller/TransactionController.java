package com.msa.transaction_service.controller;

import com.msa.transaction_service.dto.TransactionProcessRequest;
import com.msa.transaction_service.dto.TransactionProcessResponse;
import com.msa.transaction_service.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/transaction")
@RequiredArgsConstructor
public class TransactionController {

	private final TransactionService transactionService;

	@PostMapping("/deposit")
	public ResponseEntity<TransactionProcessResponse> processDeposit(@RequestBody TransactionProcessRequest request) {
		log.info("Received deposit request for userId: {}", request.getUserId());
		TransactionProcessResponse response = transactionService.processDeposit(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PostMapping("/withdrawal")
	public ResponseEntity<TransactionProcessResponse> processWithdrawal(@RequestBody TransactionProcessRequest request) {
		log.info("Received withdrawal request for userId: {}", request.getUserId());
		TransactionProcessResponse response = transactionService.processWithdrawal(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
}
