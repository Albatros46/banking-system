package com.transaction_service.services;

import com.transaction_service.client.AccountClient;
import com.transaction_service.client.dto.AccountDto;
import com.transaction_service.dtos.request.TransferRequest;
import com.transaction_service.dtos.response.TransactionResponse;
import com.transaction_service.entity.Transaction;
import com.transaction_service.enums.TransactionStatus;
import com.transaction_service.events.FraudCheckResultEvent;
import com.transaction_service.events.TransactionInitiatedEvent;
import com.transaction_service.events.TransactionStatusEvent;
import com.transaction_service.exceptions.AccountServiceException;
import com.transaction_service.exceptions.InvalidTransferException;
import com.transaction_service.exceptions.TransactionNotFoundException;
import com.transaction_service.exceptions.TransferProcessingException;
import com.transaction_service.repositories.TransactionRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/*
    Transfer SAGA (orchestration)
    1. Gonderenden dus                     -> account-service /deduct
    2. Fraud kontrolu iste                 -> "transaction-initiated"
    3. Fraud sonucunu al                   <- "fraud-check-results"
    4. Temizse aliciya yatir               -> account-service /credit   => COMPLETED
       Fraud / hata varsa gonderene iade et -> account-service /refund   => FAILED
    5. Sonucu yayinla                      -> "transaction-events"
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService {

    private static final int MAX_REASON_LENGTH = 255;

    private final TransactionRepository transactionRepository;
    private final AccountClient accountClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.transaction-initiated}")
    private String transactionInitiatedTopic;
    @Value("${app.kafka.topics.transaction-events}")
    private String transactionEventsTopic;

    public TransactionResponse initiateTransfer(TransferRequest request) {
        if (request.getFromAccountNumber().equals(request.getToAccountNumber())) {
            throw new InvalidTransferException("Auftraggeber- und Empfängerkonto müssen sich unterscheiden.");
        }
        AccountDto receiver = getAccount(request.getToAccountNumber());
        if (!"ACTIVE".equals(receiver.status())) {
            throw new InvalidTransferException("Das Empfängerkonto ist nicht aktiv: " + receiver.accountNumber());
        }

        Transaction transaction = new Transaction();
        transaction.setFromAccountNumber(request.getFromAccountNumber());
        transaction.setToAccountNumber(request.getToAccountNumber());
        transaction.setAmount(request.getAmount());
        transaction.setStatus(TransactionStatus.PENDING);
        transaction = transactionRepository.save(transaction);
        log.info("Transfer initiated : {} ({} -> {}, {})", transaction.getId(),
                transaction.getFromAccountNumber(), transaction.getToAccountNumber(), transaction.getAmount());

        // SAGA Step 1 - gonderenden dus
        try {
            accountClient.deductBalance(transaction.getFromAccountNumber(), transaction.getAmount());
        } catch (FeignException ex) {
            AccountServiceException error = AccountServiceException.from(ex);
            finish(transaction, TransactionStatus.FAILED, error.getMessage());
            throw error;
        }

        // SAGA Step 2 - fraud kontrolu icin event yayinla
        TransactionInitiatedEvent event = new TransactionInitiatedEvent(
                transaction.getId(),
                transaction.getFromAccountNumber(),
                transaction.getToAccountNumber(),
                transaction.getAmount());
        try {
            kafkaTemplate.send(transactionInitiatedTopic, transaction.getFromAccountNumber(), event)
                    .get(10, TimeUnit.SECONDS);
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Could not publish transaction-initiated event for {}", transaction.getId(), ex);
            refundSender(transaction);
            finish(transaction, TransactionStatus.FAILED, "Die Überweisung konnte nicht verarbeitet werden.");
            throw new TransferProcessingException("Die Überweisung konnte nicht verarbeitet werden. Bitte versuchen Sie es später erneut.", ex);
        }

        return mapToResponse(transaction);
    }

    // SAGA Step 3/4 - fraud sonucuna gore tamamla ya da telafi et
    public void handleFraudResult(FraudCheckResultEvent result) {
        Transaction transaction = transactionRepository.findById(result.transactionId()).orElse(null);
        if (transaction == null) {
            log.warn("Fraud result for unknown transaction : {}", result.transactionId());
            return;
        }
        // Ayni event tekrar gelirse (Kafka at-least-once) ikinci kez islem yapma
        if (transaction.getStatus() != TransactionStatus.PENDING) {
            log.info("Transaction {} already {}, ignoring fraud result", transaction.getId(), transaction.getStatus());
            return;
        }

        if (result.fraudulent()) {
            log.warn("Fraud detected for transaction {} : {}", transaction.getId(), result.reason());
            refundSender(transaction);
            finish(transaction, TransactionStatus.FAILED, "Betrugsverdacht: " + result.reason());
            return;
        }

        try {
            accountClient.creditBalance(transaction.getToAccountNumber(), transaction.getAmount());
        } catch (FeignException ex) {
            if (ex.status() <= 0) {
                // account-service'e ulasilamadi -> exception Kafka'ya doner, event tekrar denenir
                throw ex;
            }
            String reason = "Gutschrift an den Empfänger fehlgeschlagen: " + AccountServiceException.from(ex).getMessage();
            log.error("Transaction {} : {}", transaction.getId(), reason);
            refundSender(transaction);
            finish(transaction, TransactionStatus.FAILED, reason);
            return;
        }
        finish(transaction, TransactionStatus.COMPLETED, null);
    }

    public TransactionResponse getTransaction(String transactionId) {
        return transactionRepository.findById(transactionId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));
    }

    public List<TransactionResponse> getTransactionsByAccount(String accountNumber) {
        return transactionRepository
                .findByFromAccountNumberOrToAccountNumberOrderByCreatedAtDesc(accountNumber, accountNumber)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private AccountDto getAccount(String accountNumber) {
        try {
            return accountClient.getAccount(accountNumber);
        } catch (FeignException ex) {
            throw AccountServiceException.from(ex);
        }
    }

    // SAGA compensation - Step 1'i geri al. Hata olursa exception yukari firlar (Kafka tarafinda tekrar denenir)
    private void refundSender(Transaction transaction) {
        accountClient.refundBalance(transaction.getFromAccountNumber(), transaction.getAmount());
        log.info("Refunded {} to sender {} for transaction {}",
                transaction.getAmount(), transaction.getFromAccountNumber(), transaction.getId());
    }

    private void finish(Transaction transaction, TransactionStatus status, String reason) {
        transaction.setStatus(status);
        transaction.setFailureReason(truncate(reason));
        transactionRepository.save(transaction);
        log.info("Transaction {} -> {}", transaction.getId(), status);

        TransactionStatusEvent event = new TransactionStatusEvent(
                transaction.getId(),
                transaction.getFromAccountNumber(),
                transaction.getToAccountNumber(),
                transaction.getAmount(),
                status.name(),
                transaction.getFailureReason());
        kafkaTemplate.send(transactionEventsTopic, transaction.getId(), event)
                .whenComplete((sendResult, ex) -> {
                    if (ex != null) {
                        log.error("Could not publish status event for transaction {}", transaction.getId(), ex);
                    }
                });
    }

    private String truncate(String reason) {
        if (reason == null || reason.length() <= MAX_REASON_LENGTH) {
            return reason;
        }
        return reason.substring(0, MAX_REASON_LENGTH);
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        TransactionResponse response = new TransactionResponse();
        response.setId(transaction.getId());
        response.setFromAccountNumber(transaction.getFromAccountNumber());
        response.setToAccountNumber(transaction.getToAccountNumber());
        response.setAmount(transaction.getAmount());
        response.setStatus(transaction.getStatus());
        response.setFailureReason(transaction.getFailureReason());
        response.setCreatedAt(transaction.getCreatedAt());
        response.setUpdatedAt(transaction.getUpdatedAt());
        return response;
    }
}
