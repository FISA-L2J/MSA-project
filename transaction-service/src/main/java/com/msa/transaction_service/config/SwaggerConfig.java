package com.msa.transaction_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

	@Bean
	public OpenAPI openAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("Transaction Service API")
						.description("MSA Transaction Service - 잔액·거래 처리(입금/출금 실행) API")
						.version("v1.0.0"));
	}
}
