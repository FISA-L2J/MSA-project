package com.msa.account_service.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msa.account_service.constatns.NatsConstants;
import com.msa.account_service.domain.Status;
import com.msa.account_service.domain.TransactionRecord;
import com.msa.account_service.dto.TransactionProcessResponse;
import com.msa.account_service.domain.Account;
import com.msa.account_service.repository.AccountRepository;
import com.msa.account_service.repository.TransactionRecordRepository;
import io.nats.client.*;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.RetentionPolicy;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionResultSubscriber implements CommandLineRunner {

    private final Connection natsConnection;
    private final ObjectMapper objectMapper;
    private final TransactionRecordRepository transactionRecordRepository;
    private final AccountRepository accountRepository;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public void run(String... args) throws Exception {
        JetStreamManagement jsm = natsConnection.jetStreamManagement();
        JetStream js = natsConnection.jetStream();

        ensureStreams(jsm);

        PullSubscribeOptions options = PullSubscribeOptions.builder()
                .durable("account-result-consumer")
                .configuration(ConsumerConfiguration.builder()
                        .maxDeliver(3)
                        .build())
                .build();

        JetStreamSubscription subscription = js.subscribe(NatsConstants.RESULT_ALL, options);

        executorService.submit(() -> pollMessages(subscription));

        log.info("Started Pull subscriber for {}", NatsConstants.RESULT_ALL);
    }

    private void pollMessages(JetStreamSubscription subscription) {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                List<Message> messages = subscription.fetch(10, Duration.ofSeconds(1));

                for (Message msg : messages) {
                    handleResult(msg);
                }
            } catch (Exception e) {
                log.error("Error polling messages", e);
                if (e instanceof JetStreamStatusException) {
                    break;
                }
            }
        }
    }

    private void handleResult(Message msg) {
        try {
            TransactionProcessResponse response = objectMapper.readValue(
                    msg.getData(), TransactionProcessResponse.class);

            log.info("Received result - recordId: {}, status: {}",
                    response.getRecordId(), response.getStatus());

            TransactionRecord record = transactionRecordRepository
                    .findById(response.getRecordId())
                    .orElseThrow(() -> new RuntimeException(
                            "TransactionRecord not found: " + response.getRecordId()));

            if (record.getTransactionId() != null) {
                log.warn("Already processed: recordId={}, transactionId={}",
                        response.getRecordId(), record.getTransactionId());
                msg.ack();
                return;
            }

            // 상태 업데이트
            if (response.getStatus() == Status.SUCCESS) {
                record.updateStatus(Status.SUCCESS);

                // 계좌 잔액 업데이트 추가
                Account account = accountRepository.findByUserId(record.getUserId())
                        .orElseThrow(() -> new RuntimeException("Account not found for userId: " + record.getUserId()));

                if (response.getNewBalance() != null) {
                    account.setBalance(response.getNewBalance());
                    accountRepository.save(account);
                    log.info("Updated account balance for user {} to {}", record.getUserId(), response.getNewBalance());
                }
            } else {
                record.updateStatus(Status.FAILED);
            }
            record.setTransactionId(response.getTransactionId());

            transactionRecordRepository.save(record);
            log.info("Updated record {} to {}, transactionId={}",
                    record.getId(), record.getStatus(), record.getTransactionId());

            msg.ack();
        } catch (Exception e) {
            log.error("Failed to process transaction result", e);
            msg.nak();
        }
    }

    private void ensureStreams(JetStreamManagement jsm) throws Exception {
        try {
            jsm.addStream(StreamConfiguration.builder()
                    .name(NatsConstants.STREAM_REQUEST)
                    .subjects(NatsConstants.DEPOSIT, NatsConstants.WITHDRAWAL)
                    .retentionPolicy(RetentionPolicy.WorkQueue)
                    .storageType(StorageType.File)
                    .build());
            log.info("Created stream: {}", NatsConstants.STREAM_REQUEST);
        } catch (Exception e) {
            log.info("Stream {} already exists or error: {}", NatsConstants.STREAM_REQUEST, e.getMessage());
        }

        try {
            jsm.addStream(StreamConfiguration.builder()
                    .name(NatsConstants.STREAM_RESULT)
                    .subjects(NatsConstants.RESULT_ALL)
                    .retentionPolicy(RetentionPolicy.WorkQueue)
                    .storageType(StorageType.File)
                    .build());
            log.info("Created stream: {}", NatsConstants.STREAM_RESULT);
        } catch (Exception e) {
            log.info("Stream {} already exists or error: {}", NatsConstants.STREAM_RESULT, e.getMessage());
        }
    }
}
