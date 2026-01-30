package com.msa.transaction_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

	@GetMapping("/health")
	public String health() {
		return "Transaction Service is Up and Running!";
	}
}
