package com.project.serviceimplementation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.dto.BeneficiaryDto;
import com.project.entities.BankAccount;
import com.project.entities.Beneficiary;
import com.project.entities.User;
import com.project.enums.BeneficiaryStatus;
import com.project.enums.Role;
import com.project.enums.UserStatus;
import com.project.repository.BankAccountRepository;
import com.project.repository.BeneficiaryRepository;
import com.project.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class BeneficiaryServiceTest {

	@Mock
	BeneficiaryRepository repo;

	@Mock
	UserRepository userRepo;

	@Mock
	BankAccountRepository accountRepo;

	@InjectMocks
	BeneficiaryServiceImplementation service;

	@Test
	void addBeneficiary_valid() throws Exception {
		when(userRepo.findById(101)).thenReturn(Optional.of(User.builder()
				.userId(101).role(Role.ACCOUNTHOLDER).status(UserStatus.Active).build()));
		when(accountRepo.findByAccountNumber("AC1")).thenReturn(Optional.empty());
		when(repo.findByCustomerIdAndAccountNumber(101, "AC1")).thenReturn(Optional.empty());
		when(repo.save(any(Beneficiary.class))).thenAnswer(inv -> {
			Beneficiary b = inv.getArgument(0);
			if (b.getBeneficiaryId() == null) {
				b.setBeneficiaryId(1);
			}
			return b;
		});

		BeneficiaryDto dto = new BeneficiaryDto();
		dto.setCustomerId(101);
		dto.setAccountNumber("AC1");
		dto.setName("Meena");
		dto.setBankName("WealthWise Bank");

		BeneficiaryDto result = service.addBeneficiary(dto);

		assertNotNull(result.getBeneficiaryId());
		assertEquals(BeneficiaryStatus.Active, result.getStatus());
	}

	@Test
	void addBeneficiary_ownAccount_throwsException() {
		when(userRepo.findById(101)).thenReturn(Optional.of(User.builder()
				.userId(101).role(Role.ACCOUNTHOLDER).status(UserStatus.Active).build()));
		when(accountRepo.findByAccountNumber("AC1")).thenReturn(Optional.of(BankAccount.builder()
				.accountId(1).customerId(101).accountNumber("AC1").build()));

		BeneficiaryDto dto = new BeneficiaryDto();
		dto.setCustomerId(101);
		dto.setAccountNumber("AC1");
		dto.setName("Self");
		dto.setBankName("WealthWise Bank");

		assertThrows(Exception.class, () -> service.addBeneficiary(dto));
	}

	@Test
	void addBeneficiary_deletedDuplicate_reactivates() throws Exception {
		Beneficiary deleted = Beneficiary.builder().beneficiaryId(1).customerId(101).accountNumber("AC1")
				.name("Old").bankName("Old Bank").status(BeneficiaryStatus.Deleted).build();
		when(userRepo.findById(101)).thenReturn(Optional.of(User.builder()
				.userId(101).role(Role.ACCOUNTHOLDER).status(UserStatus.Active).build()));
		when(accountRepo.findByAccountNumber("AC1")).thenReturn(Optional.empty());
		when(repo.findByCustomerIdAndAccountNumber(101, "AC1")).thenReturn(Optional.of(deleted));
		when(repo.save(any(Beneficiary.class))).thenAnswer(inv -> inv.getArgument(0));

		BeneficiaryDto dto = new BeneficiaryDto();
		dto.setCustomerId(101);
		dto.setAccountNumber("AC1");
		dto.setName("Meena");
		dto.setBankName("WealthWise Bank");

		BeneficiaryDto result = service.addBeneficiary(dto);

		assertEquals(BeneficiaryStatus.Active, result.getStatus());
		assertEquals("Meena", result.getName());
	}

	@Test
	void deleteBeneficiary_softDeletes() throws Exception {
		Beneficiary existing = Beneficiary.builder().beneficiaryId(1).customerId(101).accountNumber("AC1")
				.name("Meena").bankName("WealthWise Bank").status(BeneficiaryStatus.Active).build();
		when(repo.findById(1)).thenReturn(Optional.of(existing));

		service.deleteBeneficiary(1);

		ArgumentCaptor<Beneficiary> captor = ArgumentCaptor.forClass(Beneficiary.class);
		verify(repo).save(captor.capture());
		assertEquals(BeneficiaryStatus.Deleted, captor.getValue().getStatus());
	}

	@Test
	void getByCustomer_returnsOnlyActive() {
		Beneficiary active = Beneficiary.builder().beneficiaryId(1).customerId(101).accountNumber("AC1")
				.name("Meena").bankName("WealthWise Bank").status(BeneficiaryStatus.Active).build();
		when(repo.findByCustomerIdAndStatus(eq(101), eq(BeneficiaryStatus.Active))).thenReturn(List.of(active));

		List<BeneficiaryDto> result = service.getBeneficiariesByCustomer(101);

		assertEquals(1, result.size());
		assertTrue(result.stream().allMatch(b -> b.getStatus() == BeneficiaryStatus.Active));
	}
}