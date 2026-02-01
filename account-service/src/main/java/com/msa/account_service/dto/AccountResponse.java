package com.msa.account_service.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AccountResponse {
    private Long accountId;
    private String userId;
    private Integer balance;
}
