package com.msa.transaction_service.constants;

public final class NatsConstants {
    private NatsConstants() {
    }

    // Request subjects
    public static final String DEPOSIT = "transaction.deposit";
    public static final String WITHDRAWAL = "transaction.withdrawal";

    // Result subjects
    public static final String RESULT_DEPOSIT = "transaction.result.deposit";
    public static final String RESULT_WITHDRAWAL = "transaction.result.withdrawal";

    // Stream names
    public static final String STREAM_REQUEST = "TRANSACTION_REQUEST";
}
