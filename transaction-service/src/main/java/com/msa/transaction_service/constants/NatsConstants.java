package com.msa.transaction_service.constants;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public final class NatsConstants {

    public static final String TYPE_DEPOSIT = "deposit";
    public static final String TYPE_WITHDRAWAL = "withdrawal";

    // Request subjects
    public static final String DEPOSIT = "transaction.deposit";
    public static final String WITHDRAWAL = "transaction.withdrawal";

    // Result subjects
    public static final String RESULT_DEPOSIT = "transaction.result.deposit";
    public static final String RESULT_WITHDRAWAL = "transaction.result.withdrawal";

    // Stream names
    public static final String STREAM_REQUEST = "TRANSACTION_REQUEST";
}
