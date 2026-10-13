package com.transaction_service.services;

import com.transaction_service.client.AccountClient;
import com.transaction_service.client.dto.AccountDto;
import com.transaction_service.dtos.request.TransferRequest;
import com.transaction_service.entity.Transaction;
import com.transaction_service.enums.TransactionStatus;
import com.transaction_service.events.FraudCheckResultEvent;
import com.transaction_service.events.TransactionInitiatedEvent;
import com.transaction_service.exceptions.AccountServiceException;
import com.transaction_service.exceptions.InvalidTransferException;
import com.transaction_service.repositories.TransactionRepository;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransactionServiceTest {

    private static final String FROM = "000000000001";
    private static final String TO = "000000000002";
    private static final BigDecimal AMOUNT = new BigDecimal("1000");

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AccountClient accountClient;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private TransactionService transactionService;

    private Transaction pending;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(transactionService, "transactionInitiatedTopic", "transaction-initiated");
        ReflectionTestUtils.setField(transactionService, "transactionEventsTopic", "transaction-events");
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            if (transaction.getId() == null) {
                transaction.setId("tx-1");
            }
            return transaction;
        });

        pending = new Transaction();
        pending.setId("tx-1");
        pending.setFromAccountNumber(FROM);
        pending.setToAccountNumber(TO);
        pending.setAmount(AMOUNT);
        pending.setStatus(TransactionStatus.PENDING);
        when(transactionRepository.findById("tx-1")).thenReturn(Optional.of(pending));
    }

    @Test
    void initiateDeductsAndPublishesEvent() {
        when(accountClient.getAccount(TO)).thenReturn(new AccountDto(TO, "Receiver", "ACTIVE"));

        var response = transactionService.initiateTransfer(new TransferRequest(FROM, TO, AMOUNT));

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.PENDING);
        verify(accountClient).deductBalance(FROM, AMOUNT);
        verify(kafkaTemplate).send(eq("transaction-initiated"), eq(FROM),
                eq(new TransactionInitiatedEvent("tx-1", FROM, TO, AMOUNT)));
    }

    @Test
    void initiateRejectsSameAccount() {
        assertThatThrownBy(() -> transactionService.initiateTransfer(new TransferRequest(FROM, FROM, AMOUNT)))
                .isInstanceOf(InvalidTransferException.class);
        verify(accountClient, never()).deductBalance(anyString(), any());
    }

    @Test
    void initiateMarksFailedWhenDeductFails() {
        when(accountClient.getAccount(TO)).thenReturn(new AccountDto(TO, "Receiver", "ACTIVE"));
        when(accountClient.deductBalance(FROM, AMOUNT)).thenThrow(feignError(422, "Unzureichende Deckung auf Konto " + FROM));

        assertThatThrownBy(() -> transactionService.initiateTransfer(new TransferRequest(FROM, TO, AMOUNT)))
                .isInstanceOf(AccountServiceException.class)
                .hasMessageContaining("Unzureichende Deckung")
                .extracting("status").isEqualTo(422);
        verify(kafkaTemplate, never()).send(eq("transaction-initiated"), anyString(), any());
        // Once PENDING olarak, deduct basarisiz olunca FAILED olarak kaydedilir
        ArgumentCaptor<Transaction> saved = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(saved.getValue().getFailureReason()).contains("Unzureichende Deckung");
    }

    @Test
    void cleanResultCreditsReceiver() {
        transactionService.handleFraudResult(new FraudCheckResultEvent("tx-1", false, null));

        verify(accountClient).creditBalance(TO, AMOUNT);
        verify(accountClient, never()).refundBalance(anyString(), any());
        assertThat(pending.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
    }

    @Test
    void fraudResultRefundsSender() {
        transactionService.handleFraudResult(new FraudCheckResultEvent("tx-1", true, "Zu viele Überweisungen"));

        verify(accountClient).refundBalance(FROM, AMOUNT);
        verify(accountClient, never()).creditBalance(anyString(), any());
        assertThat(pending.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(pending.getFailureReason()).contains("Zu viele Überweisungen");
    }

    @Test
    void creditFailureRefundsSender() {
        when(accountClient.creditBalance(TO, AMOUNT)).thenThrow(feignError(422, "Konto ist geschlossen: " + TO));

        transactionService.handleFraudResult(new FraudCheckResultEvent("tx-1", false, null));

        verify(accountClient).refundBalance(FROM, AMOUNT);
        assertThat(pending.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(pending.getFailureReason()).contains("Konto ist geschlossen");
    }

    @Test
    void duplicateResultIsIgnored() {
        pending.setStatus(TransactionStatus.COMPLETED);

        transactionService.handleFraudResult(new FraudCheckResultEvent("tx-1", false, null));

        verify(accountClient, never()).creditBalance(anyString(), any());
        verify(accountClient, never()).refundBalance(anyString(), any());
    }

    private static FeignException feignError(int status, String detail) {
        Request request = Request.create(Request.HttpMethod.PUT, "http://account-service", Map.of(), null,
                StandardCharsets.UTF_8, null);
        Response response = Response.builder()
                .status(status)
                .reason("error")
                .request(request)
                .headers(Map.of())
                .body("{\"detail\":\"" + detail + "\"}", StandardCharsets.UTF_8)
                .build();
        return FeignException.errorStatus("AccountClient", response);
    }
}
