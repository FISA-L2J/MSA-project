package com.msa.transaction_service.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msa.transaction_service.constants.NatsConstants;
import com.msa.transaction_service.domain.Status;
import com.msa.transaction_service.dto.TransactionProcessRequest;
import com.msa.transaction_service.dto.TransactionProcessResponse;
import com.msa.transaction_service.exception.InsufficientBalanceException;
import com.msa.transaction_service.repository.TransactionRepository;
import com.msa.transaction_service.service.TransactionService;
import io.nats.client.*;
import io.nats.client.api.ConsumerConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE)
public class TransactionEventSubscriber implements CommandLineRunner {

    private final Connection natsConnection;
    private final ObjectMapper objectMapper;
    private final TransactionService transactionService;
    private final TransactionResultPublisher resultPublisher;
    private final TransactionRepository transactionRepository;

    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    @Override
    public void run(String... args) throws Exception {
        JetStream js = natsConnection.jetStream();
        JetStreamManagement jsm = natsConnection.jetStreamManagement();

        waitForStream(jsm);

        PullSubscribeOptions depositOptions = PullSubscribeOptions.builder()
                .durable("deposit-consumer")
                .configuration(ConsumerConfiguration.builder()
                        .maxDeliver(3)
                        .build())
                .build();

        PullSubscribeOptions withdrawalOptions = PullSubscribeOptions.builder()
                .durable("withdrawal-consumer")
                .configuration(ConsumerConfiguration.builder()
                        .maxDeliver(3)
                        .build())
                .build();

        JetStreamSubscription depositSub = js.subscribe(NatsConstants.DEPOSIT, depositOptions);
        JetStreamSubscription withdrawalSub = js.subscribe(NatsConstants.WITHDRAWAL, withdrawalOptions);

        executorService.submit(() -> pollMessages(depositSub, NatsConstants.TYPE_DEPOSIT));
        executorService.submit(() -> pollMessages(withdrawalSub, NatsConstants.TYPE_WITHDRAWAL));

        log.info("Started Pull subscribers for deposit and withdrawal");
    }

    private void pollMessages(JetStreamSubscription subscription, String type) {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                List<Message> messages = subscription.fetch(10, Duration.ofSeconds(1));

                for (Message msg : messages) {
                    if (NatsConstants.TYPE_DEPOSIT.equals(type)) {
                        handleDeposit(msg);
                    } else {
                        handleWithdrawal(msg);
                    }
                }
            } catch (Exception e) {
                log.error("Error polling messages for {}", type, e);
                if (e instanceof JetStreamStatusException) {
                    break;
                }
            }
        }
    }

    private void handleDeposit(Message msg) {
        try {
            TransactionProcessRequest request = objectMapper.readValue(
                    msg.getData(), TransactionProcessRequest.class);

            if (transactionRepository.existsByRecordId(request.getRecordId())) {
                log.warn("Duplicate deposit detected: recordId={}", request.getRecordId());
                msg.ack();
                return;
            }

            log.info("Received deposit event for userId: {}", request.getUserId());

            TransactionProcessResponse response = transactionService.processDeposit(request);
            resultPublisher.publish(NatsConstants.RESULT_DEPOSIT, response);

            msg.ack();
        } catch (Exception e) {
            log.error("Failed to process deposit event", e);
            msg.nak();
        }
    }

    private void handleWithdrawal(Message msg) {
        TransactionProcessRequest request = null;
        try {
            request = objectMapper.readValue(
                    msg.getData(), TransactionProcessRequest.class);

            if (transactionRepository.existsByRecordId(request.getRecordId())) {
                log.warn("Duplicate withdrawal detected: recordId={}", request.getRecordId());
                msg.ack();
                return;
            }

            log.info("Received withdrawal event for userId: {}", request.getUserId());

            TransactionProcessResponse response = transactionService.processWithdrawal(request);
            resultPublisher.publish(NatsConstants.RESULT_WITHDRAWAL, response);

            msg.ack();
        } catch (InsufficientBalanceException e) {
            log.warn("Insufficient balance for userId: {}", request.getUserId());
            TransactionProcessResponse failedResponse = TransactionProcessResponse.builder()
                    .recordId(request.getRecordId())
                    .status(Status.FAILED)
                    .build();
            resultPublisher.publish(NatsConstants.RESULT_WITHDRAWAL, failedResponse);
            msg.ack();
        } catch (Exception e) {
            log.error("Failed to process withdrawal event", e);
            msg.nak();
        }

    }

    private void waitForStream(JetStreamManagement jsm) throws Exception {
        int maxRetries = 10;
        for (int i = 0; i < maxRetries; i++) {
            try {
                jsm.getStreamInfo(NatsConstants.STREAM_REQUEST);
                log.info("Stream {} found", NatsConstants.STREAM_REQUEST);
                return;
            } catch (Exception e) {
                log.warn("Waiting for stream... retry {}/{}", i + 1, maxRetries);
                Thread.sleep(3000);
            }
        }
        throw new RuntimeException("Stream " + NatsConstants.STREAM_REQUEST + " not found after retries");
    }
}
