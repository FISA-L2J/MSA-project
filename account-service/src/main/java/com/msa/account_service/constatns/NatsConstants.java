package com.msa.account_service.constatns;

public final class NatsConstants {
    private NatsConstants() {
    }

    // Request subjects
    public static final String DEPOSIT = "transaction.deposit";
    public static final String WITHDRAWAL = "transaction.withdrawal";

    // Result subjects
    public static final String RESULT_DEPOSIT = "transaction.result.deposit";
    public static final String RESULT_WITHDRAWAL = "transaction.result.withdrawal";
    public static final String RESULT_ALL = "transaction.result.>";

    // Stream names
    public static final String STREAM_REQUEST = "TRANSACTION_REQUEST";
    public static final String STREAM_RESULT = "TRANSACTION_RESULT";
}
