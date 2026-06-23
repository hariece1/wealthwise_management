package com.project.serviceimplementation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.dto.FundTransferDto;
import com.project.entities.BankAccount;
import com.project.entities.FundTransfer;
import com.project.enums.AccountStatus;
import com.project.enums.KycStatus;
import com.project.enums.TransferStatus;
import com.project.enums.TransferType;
import com.project.repository.AmlFlagRepository;
import com.project.repository.BankAccountRepository;
import com.project.repository.FundTransferRepository;
import com.project.repository.KycRecordRepository;
import com.project.service.AuditTrailService;
import com.project.service.LedgerService;
import com.project.service.NotifierService;

@ExtendWith(MockitoExtension.class)
class FundTransferServiceTest {

	@Mock
	FundTransferRepository repo;
	@Mock
	BankAccountRepository accountRepo;
	@Mock
	KycRecordRepository kycRepo;
	@Mock
	AmlFlagRepository amlRepo;
	@Mock
	LedgerService ledger;
	@Mock
	NotifierService notifier;
	@Mock
	AuditTrailService auditTrail;

	@InjectMocks
	FundTransferServiceImplementation service;

	@Test
	void initiateTransfer_valid_completedStatus() throws Exception {
		when(accountRepo.findById(1)).thenReturn(Optional.of(BankAccount.builder()
				.accountId(1).customerId(101).status(AccountStatus.Active).build()));
		when(accountRepo.findById(2)).thenReturn(Optional.of(BankAccount.builder()
				.accountId(2).customerId(102).status(AccountStatus.Active).build()));
		when(kycRepo.existsByCustomerIdAndStatus(eq(101), eq(KycStatus.Verified))).thenReturn(true);
		when(repo.save(any(FundTransfer.class))).thenAnswer(inv -> {
			FundTransfer f = inv.getArgument(0);
			if (f.getTransferId() == null) {
				f.setTransferId(1);
			}
			return f;
		});

		FundTransferDto result = service.initiateTransfer(1, 2, 100.0, TransferType.Internal);

		assertEquals(TransferStatus.Completed, result.getStatus());
	}

	@Test
	void initiateTransfer_sameAccount_throwsException() {
		assertThrows(Exception.class, () -> service.initiateTransfer(1, 1, 100.0, TransferType.Internal));
	}

	@Test
	void initiateTransfer_inactiveSourceAccount_throwsException() {
		when(accountRepo.findById(1)).thenReturn(Optional.of(BankAccount.builder()
				.accountId(1).customerId(101).status(AccountStatus.Closed).build()));
		when(accountRepo.findById(2)).thenReturn(Optional.of(BankAccount.builder()
				.accountId(2).customerId(102).status(AccountStatus.Active).build()));

		assertThrows(Exception.class, () -> service.initiateTransfer(1, 2, 100.0, TransferType.Internal));
	}

	@Test
	void reverseTransfer_completed_becomesReversed() throws Exception {
		FundTransfer existing = FundTransfer.builder().transferId(5).fromAccountId(1).toAccountId(2).amount(500.0)
				.status(TransferStatus.Completed).build();
		when(repo.findById(5)).thenReturn(Optional.of(existing));
		when(repo.save(any(FundTransfer.class))).thenAnswer(inv -> inv.getArgument(0));

		FundTransferDto result = service.reverseTransfer(5);

		assertEquals(TransferStatus.Reversed, result.getStatus());
	}

	@Test
	void getStatement_invalidDateRange_throwsException() {
		assertThrows(IllegalArgumentException.class,
				() -> service.getStatement(1, LocalDate.now(), LocalDate.now().minusDays(1)));
	}

	@Test
	void getTransfersByAccount_returnsCorrect() {
		FundTransfer t1 = FundTransfer.builder().transferId(1).fromAccountId(1).toAccountId(2).amount(100.0)
				.status(TransferStatus.Completed).build();
		FundTransfer t2 = FundTransfer.builder().transferId(2).fromAccountId(3).toAccountId(1).amount(200.0)
				.status(TransferStatus.Completed).build();
		when(repo.findByFromAccountIdOrToAccountId(1, 1)).thenReturn(List.of(t1, t2));

		List<FundTransferDto> result = service.getTransfersByAccount(1);

		assertEquals(2, result.size());
	}
}