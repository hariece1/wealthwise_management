package com.project.serviceimplementation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.dto.ScheduledPaymentDto;
import com.project.entities.BankAccount;
import com.project.entities.Beneficiary;
import com.project.entities.ScheduledPayment;
import com.project.enums.AccountStatus;
import com.project.enums.BeneficiaryStatus;
import com.project.enums.Frequency;
import com.project.enums.ScheduledPaymentStatus;
import com.project.repository.BankAccountRepository;
import com.project.repository.BeneficiaryRepository;
import com.project.repository.ScheduledPaymentRepository;

@ExtendWith(MockitoExtension.class)
class ScheduledPaymentServiceTest {

	@Mock
	ScheduledPaymentRepository repo;

	@Mock
	BeneficiaryRepository beneficiaryRepo;

	@Mock
	BankAccountRepository accountRepo;

	@InjectMocks
	ScheduledPaymentServiceImplementation service;

	@Test
	void createSchedule_valid_forcesActiveStatus() throws Exception {
		when(accountRepo.findById(1)).thenReturn(Optional.of(BankAccount.builder()
				.accountId(1).customerId(101).status(AccountStatus.Active).build()));
		when(beneficiaryRepo.findById(10)).thenReturn(Optional.of(Beneficiary.builder()
				.beneficiaryId(10).customerId(101).status(BeneficiaryStatus.Active).build()));
		when(repo.save(any(ScheduledPayment.class))).thenAnswer(inv -> {
			ScheduledPayment p = inv.getArgument(0);
			p.setScheduleId(5);
			return p;
		});

		ScheduledPaymentDto dto = validDto();
		dto.setStatus(ScheduledPaymentStatus.Cancelled);

		ScheduledPaymentDto result = service.createSchedule(dto);

		assertEquals(ScheduledPaymentStatus.Active, result.getStatus());
		assertEquals(5, result.getScheduleId());
	}

	@Test
	void createSchedule_sourceAccountMustBelongToCustomer() {
		when(accountRepo.findById(1)).thenReturn(Optional.of(BankAccount.builder()
				.accountId(1).customerId(999).status(AccountStatus.Active).build()));

		assertThrows(Exception.class, () -> service.createSchedule(validDto()));
	}

	@Test
	void createSchedule_beneficiaryMustBelongToCustomer() {
		when(accountRepo.findById(1)).thenReturn(Optional.of(BankAccount.builder()
				.accountId(1).customerId(101).status(AccountStatus.Active).build()));
		when(beneficiaryRepo.findById(10)).thenReturn(Optional.of(Beneficiary.builder()
				.beneficiaryId(10).customerId(999).status(BeneficiaryStatus.Active).build()));

		assertThrows(Exception.class, () -> service.createSchedule(validDto()));
	}

	@Test
	void pauseSchedule_cancelled_throwsException() {
		when(repo.findById(5)).thenReturn(Optional.of(ScheduledPayment.builder()
				.scheduleId(5).status(ScheduledPaymentStatus.Cancelled).build()));

		assertThrows(Exception.class, () -> service.pauseSchedule(5));
	}

	@Test
	void getDuePayments_onlyActiveDue() {
		ScheduledPayment due = ScheduledPayment.builder().scheduleId(1).status(ScheduledPaymentStatus.Active)
				.nextRunDate(LocalDate.now()).build();
		when(repo.findByStatusAndNextRunDateLessThanEqual(ScheduledPaymentStatus.Active, LocalDate.now()))
				.thenReturn(List.of(due));

		List<ScheduledPaymentDto> result = service.getDuePayments(LocalDate.now());

		assertEquals(1, result.size());
		assertEquals(ScheduledPaymentStatus.Active, result.get(0).getStatus());
	}

	private ScheduledPaymentDto validDto() {
		ScheduledPaymentDto dto = new ScheduledPaymentDto();
		dto.setCustomerId(101);
		dto.setFromAccountId(1);
		dto.setBeneficiaryId(10);
		dto.setAmount(1500.0);
		dto.setFrequency(Frequency.Monthly);
		dto.setNextRunDate(LocalDate.now());
		return dto;
	}
}