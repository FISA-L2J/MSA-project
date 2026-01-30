package com.msa.account_service.client;

import com.msa.account_service.dto.TransactionProcessRequest;
import com.msa.account_service.dto.TransactionProcessResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "transaction-service", url = "${transaction.service.url:http://localhost:8081}")
public interface TransactionClient {

	@PostMapping("/transaction/deposit")
	TransactionProcessResponse processDeposit(@RequestBody TransactionProcessRequest request);

	@PostMapping("/transaction/withdrawal")
	TransactionProcessResponse processWithdrawal(@RequestBody TransactionProcessRequest request);
}
