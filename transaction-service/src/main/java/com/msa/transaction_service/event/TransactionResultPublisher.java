package com.msa.transaction_service.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msa.transaction_service.dto.TransactionProcessResponse;
import io.nats.client.Connection;
import io.nats.client.JetStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionResultPublisher {

    private final Connection natsConnection;
    private final ObjectMapper objectMapper;

    public void publish(String subject, TransactionProcessResponse response) {
        try {
            JetStream js = natsConnection.jetStream();
            byte[] data = objectMapper.writeValueAsBytes(response);
            js.publish(subject, data);
            log.info("Published result '{}' - transactionId: {}, status: {}",
                    subject, response.getTransactionId(), response.getStatus());
        } catch (Exception e) {
            log.error("Failed to publish result '{}'", subject, e);
            throw new RuntimeException("NATS result publish failed: " + subject, e);
        }
    }
}