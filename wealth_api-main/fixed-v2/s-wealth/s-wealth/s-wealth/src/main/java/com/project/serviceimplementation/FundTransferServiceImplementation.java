package com.project.serviceimplementation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.dto.FundTransferDto;
import com.project.entities.AmlFlag;
import com.project.entities.BankAccount;
import com.project.entities.FundTransfer;
import com.project.enums.AccountStatus;
import com.project.enums.AmlStatus;
import com.project.enums.AuditAction;
import com.project.enums.AuditModule;
import com.project.enums.Currency;
import com.project.enums.FlagType;
import com.project.enums.KycStatus;
import com.project.enums.NotificationCategory;
import com.project.enums.Severity;
import com.project.enums.TransferStatus;
import com.project.enums.TransferType;
import com.project.repository.AmlFlagRepository;
import com.project.repository.BankAccountRepository;
import com.project.repository.FundTransferRepository;
import com.project.repository.KycRecordRepository;
import com.project.service.AuditTrailService;
import com.project.service.FundTransferService;
import com.project.service.LedgerService;
import com.project.service.NotifierService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FundTransferServiceImplementation implements FundTransferService {

	/** Transfers at or above this amount are auto-flagged for AML review. */
	private static final double LARGE_TXN_THRESHOLD = 100000.0;

	@Autowired
	FundTransferRepository repo;

	@Autowired
	BankAccountRepository accountRepo;

	@Autowired
	KycRecordRepository kycRepo;

	@Autowired
	AmlFlagRepository amlRepo;

	@Autowired
	LedgerService ledger;

	@Autowired
	NotifierService notifier;

	@Autowired
	AuditTrailService auditTrail;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public FundTransferDto initiateTransfer(Integer fromAccountId, Integer toAccountId, Double amount, TransferType type)
			throws Exception {
		if (fromAccountId == null || toAccountId == null) {
			throw new Exception("Both fromAccountId and toAccountId are required.");
		}
		if (fromAccountId.equals(toAccountId)) {
			throw new Exception("Source and destination accounts must be different.");
		}
		if (amount == null || amount <= 0) {
			throw new Exception("Transfer amount must be greater than zero.");
		}

		// cross-module: accounts must exist (Customer Account module)
		BankAccount from = accountRepo.findById(fromAccountId)
				.orElseThrow(() -> new Exception("Source account not found with ID: " + fromAccountId));
		BankAccount to = accountRepo.findById(toAccountId)
				.orElseThrow(() -> new Exception("Destination account not found with ID: " + toAccountId));
		if (from.getStatus() != AccountStatus.Active) {
			throw new Exception("Source account is not active.");
		}
		if (to.getStatus() != AccountStatus.Active) {
			throw new Exception("Destination account is not active.");
		}

		// cross-module: sender's KYC must be Verified (Compliance module)
		if (!kycRepo.existsByCustomerIdAndStatus(from.getCustomerId(), KycStatus.Verified)) {
			throw new Exception("Sender's KYC is not Verified; transfer blocked.");
		}

		// save as INITIATED
		FundTransfer e = FundTransfer.builder()
				.fromAccountId(fromAccountId)
				.toAccountId(toAccountId)
				.amount(amount)
				.currency(Currency.INR)
				.transferType(type == null ? TransferType.Internal : type)
				.transferDate(LocalDate.now())
				.status(TransferStatus.Initiated)
				.build();
		e = repo.save(e);

		// move the money through the ledger (updates balances + writes AccountStatement rows)
		ledger.debit(fromAccountId, amount);
		ledger.credit(toAccountId, amount);

		// mark COMPLETED
		e.setStatus(TransferStatus.Completed);
		e = repo.save(e);
		log.info("Transfer {} completed: {} -> {} amount {}", e.getTransferId(), fromAccountId, toAccountId, amount);

		// cross-module: auto-raise an AML flag for large transfers (Compliance module)
		if (amount >= LARGE_TXN_THRESHOLD) {
			amlRepo.save(AmlFlag.builder()
					.bankAccount(from)
					.transactionId(e.getTransferId())
					.flagType(FlagType.LargeTransaction)
					.severity(Severity.High)
					.raisedDate(LocalDate.now())
					.status(AmlStatus.Open)
					.build());
			log.info("AML flag raised for large transfer {}", e.getTransferId());
		}

		// cross-module: notify both customers (Notifications module)
		notifier.notify(from.getCustomerId(), "Your account was debited " + amount + " (transfer #" + e.getTransferId() + ").",
				NotificationCategory.Transaction);
		notifier.notify(to.getCustomerId(), "Your account was credited " + amount + " (transfer #" + e.getTransferId() + ").",
				NotificationCategory.Transaction);

		// cross-module: audit trail (IAM module)
		auditTrail.record(AuditAction.TRANSFER, AuditModule.TRANSFER_MODULE);

		return toDto(e);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public FundTransferDto reverseTransfer(Integer id) throws Exception {
		FundTransfer e = repo.findById(id).orElseThrow(() -> new Exception("FundTransfer not found with ID: " + id));
		if (e.getStatus() != TransferStatus.Completed) {
			throw new Exception("Only a COMPLETED transfer can be reversed.");
		}
		// undo the money movement: credit the sender, debit the receiver
		ledger.debit(e.getToAccountId(), e.getAmount());
		ledger.credit(e.getFromAccountId(), e.getAmount());

		e.setStatus(TransferStatus.Reversed);
		e = repo.save(e);

		BankAccount from = accountRepo.findById(e.getFromAccountId()).orElse(null);
		BankAccount to = accountRepo.findById(e.getToAccountId()).orElse(null);
		if (from != null) {
			notifier.notify(from.getCustomerId(), "Transfer #" + id + " was reversed; " + e.getAmount() + " credited back.",
					NotificationCategory.Transaction);
		}
		if (to != null) {
			notifier.notify(to.getCustomerId(), "Transfer #" + id + " was reversed; " + e.getAmount() + " debited.",
					NotificationCategory.Transaction);
		}
		auditTrail.record(AuditAction.TRANSFER, AuditModule.TRANSFER_MODULE);
		log.info("Transfer {} reversed", id);
		return toDto(e);
	}

	@Override
	public FundTransferDto getById(Integer id) throws Exception {
		FundTransfer e = repo.findById(id).orElseThrow(() -> new Exception("FundTransfer not found with ID: " + id));
		return toDto(e);
	}

	@Override
	public List<FundTransferDto> getByFromAccountId(Integer fromAccountId) {
		return toDtoList(repo.findByFromAccountId(fromAccountId));
	}

	@Override
	public List<FundTransferDto> getByToAccountId(Integer toAccountId) {
		return toDtoList(repo.findByToAccountId(toAccountId));
	}

	@Override
	public List<FundTransferDto> getTransfersByAccount(Integer accountId) {
		return toDtoList(repo.findByFromAccountIdOrToAccountId(accountId, accountId));
	}

	@Override
	public List<FundTransferDto> getStatement(Integer accountId, LocalDate from, LocalDate to) {
		if (accountId == null || from == null || to == null) {
			throw new IllegalArgumentException("accountId, from, and to are required.");
		}
		if (from.isAfter(to)) {
			throw new IllegalArgumentException("from date cannot be after to date.");
		}
		return toDtoList(repo.findStatement(accountId, from, to));
	}

	private List<FundTransferDto> toDtoList(List<FundTransfer> list) {
		List<FundTransferDto> dtos = new ArrayList<>();
		for (FundTransfer e : list) {
			dtos.add(toDto(e));
		}
		return dtos;
	}

	private FundTransferDto toDto(FundTransfer e) {
		FundTransferDto dto = new FundTransferDto();
		dto.setTransferId(e.getTransferId());
		dto.setFromAccountId(e.getFromAccountId());
		dto.setToAccountId(e.getToAccountId());
		dto.setAmount(e.getAmount());
		dto.setCurrency(e.getCurrency());
		dto.setTransferType(e.getTransferType());
		dto.setRemarks(e.getRemarks());
		dto.setTransferDate(e.getTransferDate());
		dto.setStatus(e.getStatus());
		return dto;
	}
}
