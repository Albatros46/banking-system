package com.account_service.services;

import com.account_service.entity.Account;
import com.account_service.enums.AccountStatus;
import com.account_service.enums.AccountType;
import com.account_service.exceptions.InvalidAccountOperationException;
import com.account_service.repositories.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    private static final String ACCOUNT_NUMBER = "000000000001";

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    private Account account;

    @BeforeEach
    void setUp() {
        account = new Account();
        account.setAccountNumber(ACCOUNT_NUMBER);
        account.setAccountType(AccountType.SAVING);
        account.setStatus(AccountStatus.ACTIVE);
        account.setBalance(new BigDecimal("150000"));
        account.setDailyTransactionLimit(new BigDecimal("100000"));
        when(accountRepository.findByAccountNumberForUpdate(ACCOUNT_NUMBER)).thenReturn(Optional.of(account));
    }

    @Test
    void deductUpdatesBalanceAndDailyTotal() {
        accountService.deductBalance(ACCOUNT_NUMBER, new BigDecimal("30000"));

        assertThat(account.getBalance()).isEqualByComparingTo("120000");
        assertThat(account.getDailyTransactionTotal()).isEqualByComparingTo("30000");
        assertThat(account.getLastTransactionDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void deductRejectsWhenDailyLimitExceeded() {
        accountService.deductBalance(ACCOUNT_NUMBER, new BigDecimal("80000"));

        assertThatThrownBy(() -> accountService.deductBalance(ACCOUNT_NUMBER, new BigDecimal("20000.01")))
                .isInstanceOf(InvalidAccountOperationException.class)
                .hasMessageContaining("Tageslimit überschritten");
        assertThat(account.getBalance()).isEqualByComparingTo("70000");
    }

    @Test
    void dailyTotalResetsOnNewDay() {
        account.setDailyTransactionTotal(new BigDecimal("100000"));
        account.setLastTransactionDate(LocalDate.now().minusDays(1));

        accountService.deductBalance(ACCOUNT_NUMBER, new BigDecimal("50000"));

        assertThat(account.getDailyTransactionTotal()).isEqualByComparingTo("50000");
    }

    @Test
    void deductRejectsInsufficientBalance() {
        account.setBalance(new BigDecimal("100"));

        assertThatThrownBy(() -> accountService.deductBalance(ACCOUNT_NUMBER, new BigDecimal("100.01")))
                .isInstanceOf(InvalidAccountOperationException.class)
                .hasMessageContaining("Unzureichende Deckung");
    }

    @Test
    void deductRejectsBlockedAccount() {
        account.setStatus(AccountStatus.BLOCKED);

        assertThatThrownBy(() -> accountService.deductBalance(ACCOUNT_NUMBER, BigDecimal.TEN))
                .isInstanceOf(InvalidAccountOperationException.class)
                .hasMessageContaining("ist gesperrt");
    }

    @Test
    void refundRestoresBalanceAndDailyUsage() {
        accountService.deductBalance(ACCOUNT_NUMBER, new BigDecimal("60000"));

        accountService.refundBalance(ACCOUNT_NUMBER, new BigDecimal("60000"));

        assertThat(account.getBalance()).isEqualByComparingTo("150000");
        assertThat(account.getDailyTransactionTotal()).isEqualByComparingTo("0");
        // Iade sonrasi limit tekrar kullanilabilir
        accountService.deductBalance(ACCOUNT_NUMBER, new BigDecimal("100000"));
    }

    @Test
    void refundIsAllowedForBlockedAccount() {
        account.setStatus(AccountStatus.BLOCKED);

        accountService.refundBalance(ACCOUNT_NUMBER, new BigDecimal("500"));

        assertThat(account.getBalance()).isEqualByComparingTo("150500");
    }
}
