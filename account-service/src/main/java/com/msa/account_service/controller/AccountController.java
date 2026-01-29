package com.msa.account_service.controller;

import com.msa.account_service.dto.DepositRequest;
import com.msa.account_service.dto.DepositResponse;
import com.msa.account_service.dto.WithdrawalRequest;
import com.msa.account_service.dto.WithdrawalResponse;
import com.msa.account_service.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

	private final AccountService accountService;

	@PostMapping("/deposit")
	public ResponseEntity<DepositResponse> deposit(@RequestBody @Valid DepositRequest request) {
		log.info("Received deposit request");
		DepositResponse response = accountService.deposit(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PostMapping("/withdrawal")
	public ResponseEntity<WithdrawalResponse> withdrawal(@RequestBody @Valid WithdrawalRequest request) {
		log.info("Received withdrawal request");
		WithdrawalResponse response = accountService.withdrawal(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
}
