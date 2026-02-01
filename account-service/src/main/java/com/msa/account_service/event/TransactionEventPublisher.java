package com.msa.account_service.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msa.account_service.dto.TransactionProcessRequest;
import io.nats.client.Connection;
import io.nats.client.JetStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventPublisher {

    private final Connection natsConnection;
    private final ObjectMapper objectMapper;

    private static final String DEPOSIT_SUBJECT = "transaction.deposit";
    private static final String WITHDRAWAL_SUBJECT = "transaction.withdrawal";

    // Account Service 에서 호출하는 메서드
    public void publishDeposit(TransactionProcessRequest request) {
        publish(DEPOSIT_SUBJECT, request);
    }

    public void publishWithdrawal(TransactionProcessRequest request) {
        publish(WITHDRAWAL_SUBJECT, request);
    }

    private void publish(String subject, TransactionProcessRequest request) {
        try {
            JetStream js = natsConnection.jetStream();
            byte[] data = objectMapper.writeValueAsBytes(request);
            js.publish(subject, data);
            log.info("Published event '{}' for userId: {}, amount: {}", subject, request.getUserId(), request.getAmount());
        } catch (Exception e) {
            log.error("Failed to publish event [{}]", subject, e);
            throw new RuntimeException("NATS publish failed: " + subject, e);
        }
    }
}
