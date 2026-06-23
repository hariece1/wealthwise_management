package com.project.serviceimplementation;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
import com.project.service.BeneficiaryService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BeneficiaryServiceImplementation implements BeneficiaryService {

	@Autowired
	BeneficiaryRepository repo;

	@Autowired
	UserRepository userRepo;

	@Autowired
	BankAccountRepository accountRepo;

	@Override
	public BeneficiaryDto addBeneficiary(BeneficiaryDto dto) throws Exception {
		if (dto == null) {
			throw new Exception("Beneficiary details are required.");
		}
		if (dto.getCustomerId() == null) {
			throw new Exception("customerId is required.");
		}
		if (isBlank(dto.getAccountNumber())) {
			throw new Exception("Beneficiary accountNumber is required.");
		}
		if (isBlank(dto.getName())) {
			throw new Exception("Beneficiary name is required.");
		}
		if (isBlank(dto.getBankName())) {
			throw new Exception("Beneficiary bankName is required.");
		}

		User customer = userRepo.findById(dto.getCustomerId())
				.orElseThrow(() -> new Exception("Customer not found with ID: " + dto.getCustomerId()));
		if (customer.getRole() != Role.ACCOUNTHOLDER) {
			throw new Exception("Beneficiaries can only be added for ACCOUNTHOLDER users.");
		}
		if (customer.getStatus() != UserStatus.Active) {
			throw new Exception("Beneficiary cannot be added for a non-active user.");
		}

		String accountNumber = dto.getAccountNumber().trim();
		BankAccount internalAccount = accountRepo.findByAccountNumber(accountNumber).orElse(null);
		if (internalAccount != null && dto.getCustomerId().equals(internalAccount.getCustomerId())) {
			throw new Exception("Customer cannot add their own account as a beneficiary.");
		}

		Beneficiary existing = repo.findByCustomerIdAndAccountNumber(dto.getCustomerId(), accountNumber).orElse(null);
		if (existing != null) {
			if (existing.getStatus() == BeneficiaryStatus.Deleted) {
				existing.setName(dto.getName().trim());
				existing.setBankName(dto.getBankName().trim());
				existing.setStatus(BeneficiaryStatus.Active);
				Beneficiary res = repo.save(existing);
				log.info("Beneficiary {} reactivated for customer {}", res.getBeneficiaryId(), res.getCustomerId());
				return toDto(res);
			}
			throw new Exception("This beneficiary account number is already registered for this customer.");
		}

		Beneficiary e = Beneficiary.builder()
				.customerId(dto.getCustomerId())
				.accountNumber(accountNumber)
				.name(dto.getName().trim())
				.bankName(dto.getBankName().trim())
				.status(BeneficiaryStatus.Active)
				.build();
		Beneficiary res = repo.save(e);
		log.info("Beneficiary {} added for customer {}", res.getBeneficiaryId(), res.getCustomerId());
		return toDto(res);
	}

	@Override
	public List<BeneficiaryDto> getBeneficiariesByCustomer(Integer customerId) {
		List<BeneficiaryDto> dtos = new ArrayList<>();
		for (Beneficiary e : repo.findByCustomerIdAndStatus(customerId, BeneficiaryStatus.Active)) {
			dtos.add(toDto(e));
		}
		return dtos;
	}

	@Override
	public BeneficiaryDto getById(Integer id) throws Exception {
		Beneficiary e = repo.findById(id).orElseThrow(() -> new Exception("Beneficiary not found with ID: " + id));
		return toDto(e);
	}

	@Override
	public void deleteBeneficiary(Integer id) throws Exception {
		Beneficiary e = repo.findById(id).orElseThrow(() -> new Exception("Beneficiary not found with ID: " + id));
		if (e.getStatus() == BeneficiaryStatus.Deleted) {
			return;
		}
		e.setStatus(BeneficiaryStatus.Deleted);
		repo.save(e);
		log.info("Beneficiary {} soft-deleted", id);
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	private BeneficiaryDto toDto(Beneficiary e) {
		BeneficiaryDto dto = new BeneficiaryDto();
		dto.setBeneficiaryId(e.getBeneficiaryId());
		dto.setCustomerId(e.getCustomerId());
		dto.setAccountNumber(e.getAccountNumber());
		dto.setName(e.getName());
		dto.setBankName(e.getBankName());
		dto.setStatus(e.getStatus());
		return dto;
	}
}