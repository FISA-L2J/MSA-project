package com.msa.transaction_service.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msa.transaction_service.constants.NatsConstants;
import com.msa.transaction_service.domain.Status;
import com.msa.transaction_service.dto.TransactionProcessRequest;
import com.msa.transaction_service.dto.TransactionProcessResponse;
import com.msa.transaction_service.exception.InsufficientBalanceException;
import com.msa.transaction_service.service.TransactionService;
import io.nats.client.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE)
public class TransactionEventSubscriber implements CommandLineRunner {

    private final Connection natsConnection;
    private final ObjectMapper objectMapper;
    private final TransactionService transactionService;
    private final TransactionResultPublisher resultPublisher;

    @Override
    public void run(String... args) throws Exception {
        JetStream js = natsConnection.jetStream();
        JetStreamManagement jsm = natsConnection.jetStreamManagement();
        Dispatcher dispatcher = natsConnection.createDispatcher();

        waitForStream(jsm);

        js.subscribe(NatsConstants.DEPOSIT, dispatcher, this::handleDeposit, false);
        log.info("Subscribed to {}", NatsConstants.DEPOSIT);

        js.subscribe(NatsConstants.WITHDRAWAL, dispatcher, this::handleWithdrawal, false);
        log.info("Subscribed to {}", NatsConstants.WITHDRAWAL);
    }

    private void handleDeposit(Message msg) {
        try {
            TransactionProcessRequest request = objectMapper.readValue(
                    msg.getData(), TransactionProcessRequest.class);
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
