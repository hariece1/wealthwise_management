package com.project.serviceimplementation;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.project.dto.BankAccountDto;
import com.project.entities.BankAccount;
import com.project.entities.User;
import com.project.enums.AccountStatus;
import com.project.enums.AccountType;
import com.project.enums.Role;
import com.project.enums.UserStatus;
import com.project.repository.BankAccountRepository;
import com.project.repository.UserRepository;
import com.project.service.BankAccountService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BankAccountServiceImplementation implements BankAccountService {

	private static final SecureRandom ACCOUNT_NUMBER_RANDOM = new SecureRandom();
	private static final int MAX_ACCOUNT_NUMBER_ATTEMPTS = 10;

	@Autowired
	BankAccountRepository repo;

	@Autowired
	UserRepository userRepo;

	@Override
	public BankAccountDto openAccount(BankAccountDto dto) throws Exception {
		if (dto == null) {
			throw new Exception("Bank account details are required.");
		}
		if (dto.getCustomerId() == null) {
			throw new Exception("customerId is required.");
		}
		if (dto.getAccountType() == null) {
			throw new Exception("accountType is required.");
		}
		if (dto.getBalance() != null && dto.getBalance() < 0) {
			throw new Exception("Opening balance cannot be negative.");
		}

		User customer = userRepo.findById(dto.getCustomerId())
				.orElseThrow(() -> new Exception("Customer not found with ID: " + dto.getCustomerId()));
		if (customer.getRole() != Role.ACCOUNTHOLDER) {
			throw new Exception("Bank accounts can only be opened for ACCOUNTHOLDER users.");
		}
		if (customer.getStatus() != UserStatus.Active) {
			throw new Exception("Bank account cannot be opened for a non-active user.");
		}

		BankAccount e = BankAccount.builder()
				.customerId(dto.getCustomerId())
				.accountType(dto.getAccountType())
				.accountNumber(generateAccountNumber(dto.getAccountType()))
				.balance(dto.getBalance() == null ? 0.0 : dto.getBalance())
				.interestRate(dto.getInterestRate())
				.openDate(LocalDate.now())
				.status(AccountStatus.Active)
				.build();
		BankAccount res = repo.save(e);
		log.info("Bank account {} opened ({}) for customer {}", res.getAccountId(), res.getAccountNumber(), res.getCustomerId());
		return toDto(res);
	}

	@Override
	public BankAccountDto getById(Integer id) throws Exception {
		BankAccount e = repo.findById(id).orElseThrow(() -> new Exception("BankAccount not found with ID: " + id));
		return toDto(e);
	}

	@Override
	public BankAccountDto getAccountByNumber(String accountNumber) throws Exception {
		BankAccount e = repo.findByAccountNumber(accountNumber)
				.orElseThrow(() -> new Exception("BankAccount not found with number: " + accountNumber));
		return toDto(e);
	}

	@Override
	public List<BankAccountDto> getAccountsByCustomer(Integer customerId) {
		List<BankAccountDto> dtos = new ArrayList<>();
		for (BankAccount e : repo.findByCustomerId(customerId)) {
			dtos.add(toDto(e));
		}
		return dtos;
	}

	@Override
	public BankAccountDto updateBalance(Integer accountId, double amount, boolean credit) throws Exception {
		if (amount <= 0) {
			throw new Exception("Amount must be greater than zero.");
		}
		BankAccount e = repo.findById(accountId).orElseThrow(() -> new Exception("BankAccount not found with ID: " + accountId));
		if (e.getStatus() != AccountStatus.Active) {
			throw new Exception("Account " + accountId + " is not active.");
		}
		double balance = e.getBalance() == null ? 0.0 : e.getBalance();
		if (credit) {
			balance += amount;
		} else {
			if (balance < amount) {
				throw new Exception("Insufficient balance in account " + accountId + ".");
			}
			balance -= amount;
		}
		e.setBalance(balance);
		BankAccount res = repo.save(e);
		log.info("Account {} {} {} -> balance {}", accountId, credit ? "credited" : "debited", amount, balance);
		return toDto(res);
	}

	@Override
	public BankAccountDto closeAccount(Integer accountId) throws Exception {
		BankAccount e = repo.findById(accountId).orElseThrow(() -> new Exception("BankAccount not found with ID: " + accountId));
		e.setStatus(AccountStatus.Closed);
		BankAccount res = repo.save(e);
		log.info("Account {} closed", accountId);
		return toDto(res);
	}

	private String generateAccountNumber(AccountType accountType) throws Exception {
		String prefix = switch (accountType) {
			case Savings -> "SB";
			case Current -> "CA";
			case SalaryAccount -> "SA";
		};
		String datePart = LocalDate.now().toString().replace("-", "");
		for (int i = 0; i < MAX_ACCOUNT_NUMBER_ATTEMPTS; i++) {
			String randomPart = String.format("%06d", ACCOUNT_NUMBER_RANDOM.nextInt(1_000_000));
			String accountNumber = prefix + datePart + randomPart;
			if (!repo.existsByAccountNumber(accountNumber)) {
				return accountNumber;
			}
		}
		throw new Exception("Unable to generate a unique account number. Please try again.");
	}

	private BankAccountDto toDto(BankAccount e) {
		BankAccountDto dto = new BankAccountDto();
		dto.setAccountId(e.getAccountId());
		dto.setCustomerId(e.getCustomerId());
		dto.setAccountType(e.getAccountType());
		dto.setAccountNumber(e.getAccountNumber());
		dto.setBalance(e.getBalance());
		dto.setInterestRate(e.getInterestRate());
		dto.setOpenDate(e.getOpenDate());
		dto.setStatus(e.getStatus());
		return dto;
	}
}
