package com.account_service.services;

import com.account_service.dtos.request.CreateAccountRequest;
import com.account_service.dtos.response.AccountResponse;
import com.account_service.entity.Account;
import com.account_service.enums.AccountStatus;
import com.account_service.enums.AccountType;
import com.account_service.exceptions.AccountAlreadyExistsException;
import com.account_service.exceptions.AccountNotFoundException;
import com.account_service.exceptions.InvalidAccountOperationException;
import com.account_service.repositories.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private static final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        log.info("Creating account for : {} ", request.getEmail());
        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new AccountAlreadyExistsException(request.getEmail());
        }
        Account account= new Account();
        account.setAccountHolderName(request.getAccountHolderName());
        account.setEmail(request.getEmail());
        account.setPhone(request.getPhone());
        account.setAccountType(request.getAccountType());
        account.setStatus(AccountStatus.ACTIVE);
        account.setBalance(request.getInitialDeposit());
        account.setAccountNumber(generateAccountNumber()); //yeni bir account numara olustumaliyiz. 12 digit ve uniq olmak zorunda.
        account.setDailyTransactionLimit(
                request.getAccountType()== AccountType.SAVING
                        ? new BigDecimal("100000")
                        : new BigDecimal("200000")
        );

        // flush -> INSERT hemen calisir, @CreationTimestamp (createdAt) yanitta dolu gelir
        Account savedAccount = accountRepository.saveAndFlush(account);
        log.info("Account created : {}",savedAccount.getAccountNumber());

        return mapToResponse(savedAccount);
    }
    // Generate uniq 12 digit account number
    private String generateAccountNumber() {
        String accountNumber;
        do{
            long number=secureRandom.nextLong(1_000_000_000_000L);
            accountNumber=String.format("%012d",number); //12 digit account number olacak d->decimel
        }
        while(accountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
    }

    private AccountResponse mapToResponse(Account account) {
        AccountResponse response = new AccountResponse();
        response.setId(account.getId());
        response.setAccountHolderName(account.getAccountHolderName());
        response.setAccountNumber(account.getAccountNumber());
        response.setEmail(account.getEmail());
        response.setPhone(account.getPhone());
        response.setAccountType(account.getAccountType());
        response.setStatus(account.getStatus());
        response.setBalance(account.getBalance());
        response.setDailyTransactionLimit(account.getDailyTransactionLimit());
        response.setCreatedAt(account.getCreatedAt());

        return response;
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(String accountNumber) {
        return mapToResponse(findAccount(accountNumber));
    }

    @Transactional(readOnly = true)
    public BigDecimal getBalance(String accountNumber) {
        return findAccount(accountNumber).getBalance();
    }

    @Transactional
    public void blockAccount(String accountNumber) {
        Account account = findAccountForUpdate(accountNumber);
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new InvalidAccountOperationException("Ein geschlossenes Konto kann nicht gesperrt werden: " + accountNumber);
        }
        account.setStatus(AccountStatus.BLOCKED);
        log.info("Account blocked : {}", accountNumber);
    }

    // SAGA Step 1 - gonderenin bakiyesinden dus
    @Transactional
    public void deductBalance(String accountNumber, BigDecimal amount) {
        validateAmount(amount);
        Account account = findAccountForUpdate(accountNumber);
        ensureActive(account);
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InvalidAccountOperationException("Unzureichende Deckung auf Konto " + accountNumber);
        }
        BigDecimal newDailyTotal = todaysTotal(account).add(amount);
        if (newDailyTotal.compareTo(account.getDailyTransactionLimit()) > 0) {
            throw new InvalidAccountOperationException("Tageslimit überschritten für Konto " + accountNumber);
        }
        account.setBalance(account.getBalance().subtract(amount));
        account.setDailyTransactionTotal(newDailyTotal);
        account.setLastTransactionDate(LocalDate.now());
        log.info("Deducted {} from account : {}", amount, accountNumber);
    }

    // SAGA compensation - basarisiz transferde gonderene iade et, gunluk kullanimi da geri al
    @Transactional
    public void refundBalance(String accountNumber, BigDecimal amount) {
        validateAmount(amount);
        Account account = findAccountForUpdate(accountNumber);
        account.setBalance(account.getBalance().add(amount));
        // Iade ayni gun icindeyse gunluk limitten dusulen tutari geri ver
        BigDecimal todaysTotal = todaysTotal(account);
        if (todaysTotal.signum() > 0) {
            account.setDailyTransactionTotal(todaysTotal.subtract(amount).max(BigDecimal.ZERO));
        }
        log.info("Refunded {} to account : {}", amount, accountNumber);
    }

    // SAGA Step 4 - transfer tamamlandi, aliciya yatir
    @Transactional
    public void creditBalance(String accountNumber, BigDecimal amount) {
        validateAmount(amount);
        Account account = findAccountForUpdate(accountNumber);
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new InvalidAccountOperationException("Konto ist geschlossen: " + accountNumber);
        }
        account.setBalance(account.getBalance().add(amount));
        log.info("Credited {} to account : {}", amount, accountNumber);
    }

    private Account findAccount(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    private Account findAccountForUpdate(String accountNumber) {
        return accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    private void ensureActive(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            String status = account.getStatus() == AccountStatus.BLOCKED ? "gesperrt" : "geschlossen";
            throw new InvalidAccountOperationException("Konto " + account.getAccountNumber() + " ist " + status + ".");
        }
    }

    // Son islem bugun degilse gunluk toplam sifirdan baslar
    private BigDecimal todaysTotal(Account account) {
        if (!LocalDate.now().equals(account.getLastTransactionDate()) || account.getDailyTransactionTotal() == null) {
            return BigDecimal.ZERO;
        }
        return account.getDailyTransactionTotal();
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidAccountOperationException("Der Betrag muss positiv sein.");
        }
    }
}
