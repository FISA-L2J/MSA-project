package com.msa.transaction_service.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msa.transaction_service.dto.TransactionProcessRequest;
import com.msa.transaction_service.dto.TransactionProcessResponse;
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

        js.subscribe("transaction.deposit", dispatcher, this::handleDeposit, false);
        log.info("Subscribed to transaction.deposit");

        js.subscribe("transaction.withdrawal", dispatcher, this::handleWithdrawal, false);
        log.info("Subscribed to transaction.withdrawal");
    }

    private void handleDeposit(Message msg) {
        try {
            TransactionProcessRequest request = objectMapper.readValue(
                    msg.getData(), TransactionProcessRequest.class);
            log.info("Received deposit event for userId: {}", request.getUserId());

            TransactionProcessResponse response = transactionService.processDeposit(request);
            resultPublisher.publish("transaction.result.deposit", response);

            msg.ack();
        } catch (Exception e) {
            log.error("Failed to process deposit event", e);
            msg.nak();
        }
    }

    private void handleWithdrawal(Message msg) {
        try {
            TransactionProcessRequest request = objectMapper.readValue(
                    msg.getData(), TransactionProcessRequest.class);
            log.info("Received withdrawal event for userId: {}", request.getUserId());

            TransactionProcessResponse response = transactionService.processWithdrawal(request);
            resultPublisher.publish("transaction.result.withdrawal", response);

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
                jsm.getStreamInfo("TRANSACTION_REQUEST");  // ✅ 다른 스트림 이름
                log.info("Stream TRANSACTION_REQUEST found");
                return;
            } catch (Exception e) {
                log.warn("Waiting for stream... retry {}/{}", i + 1, maxRetries);
                Thread.sleep(3000);
            }
        }
        throw new RuntimeException("Stream TRANSACTION_REQUEST not found after retries");
    }
}