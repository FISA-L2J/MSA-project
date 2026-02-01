package com.msa.account_service.config;

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
						.title("Account Service API")
						.description("MSA Account Service - 계좌 거래(입금/출금) 요청 API")
						.version("v1.0.0"));
	}
}
