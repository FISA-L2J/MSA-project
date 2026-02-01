package com.msa.account_service.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msa.account_service.domain.Status;
import com.msa.account_service.domain.TransactionRecord;
import com.msa.account_service.dto.TransactionProcessResponse;
import com.msa.account_service.repository.TransactionRecordRepository;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.JetStream;
import io.nats.client.JetStreamManagement;
import io.nats.client.api.RetentionPolicy;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionResultSubscriber implements CommandLineRunner {

    private final Connection natsConnection;
    private final ObjectMapper objectMapper;
    private final TransactionRecordRepository transactionRecordRepository;

    @Override
    public void run(String... args) throws Exception {
        JetStreamManagement jsm = natsConnection.jetStreamManagement();
        JetStream js = natsConnection.jetStream();
        Dispatcher dispatcher = natsConnection.createDispatcher();

        ensureStreams(jsm);

        js.subscribe("transaction.result.>", dispatcher, msg -> {
            try {
                TransactionProcessResponse response = objectMapper.readValue(
                        msg.getData(), TransactionProcessResponse.class);

                log.info("Received result - recordId: {}, status: {}",
                        response.getRecordId(), response.getStatus());

                TransactionRecord record = transactionRecordRepository
                        .findById(response.getRecordId())
                        .orElseThrow(() -> new RuntimeException(
                                "TransactionRecord not found: " + response.getRecordId()));

                if (response.getStatus() == Status.SUCCESS) {
                    record.updateStatus(Status.SUCCESS);
                } else {
                    record.updateStatus(Status.FAILED);
                }

                transactionRecordRepository.save(record);
                log.info("Updated record {} to {}", record.getId(), record.getStatus());

                msg.ack();
            } catch (Exception e) {
                log.error("Failed to process transaction result", e);
                msg.nak();
            }
        }, false);

        log.info("Subscribed to transaction.result.>");
    }

    private void ensureStreams(JetStreamManagement jsm) throws Exception {
        try {
            jsm.addStream(StreamConfiguration.builder()
                    .name("TRANSACTION_REQUEST")
                    .subjects("transaction.deposit", "transaction.withdrawal")
                    .retentionPolicy(RetentionPolicy.WorkQueue)
                    .storageType(StorageType.File)
                    .build());
            log.info("Created stream: TRANSACTION_REQUEST");
        } catch (Exception e) {
            log.info("Stream TRANSACTION_REQUEST already exists or error: {}", e.getMessage());
        }

        try {
            jsm.addStream(StreamConfiguration.builder()
                    .name("TRANSACTION_RESULT")
                    .subjects("transaction.result.>")
                    .retentionPolicy(RetentionPolicy.WorkQueue)
                    .storageType(StorageType.File)
                    .build());
            log.info("Created stream: TRANSACTION_RESULT");
        } catch (Exception e) {
            log.info("Stream TRANSACTION_RESULT already exists or error: {}", e.getMessage());
        }
    }
}