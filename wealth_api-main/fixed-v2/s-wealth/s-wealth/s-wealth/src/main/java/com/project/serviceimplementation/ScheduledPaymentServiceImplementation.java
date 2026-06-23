package com.project.serviceimplementation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.project.dto.ScheduledPaymentDto;
import com.project.entities.BankAccount;
import com.project.entities.Beneficiary;
import com.project.entities.ScheduledPayment;
import com.project.enums.AccountStatus;
import com.project.enums.BeneficiaryStatus;
import com.project.enums.ScheduledPaymentStatus;
import com.project.repository.BankAccountRepository;
import com.project.repository.BeneficiaryRepository;
import com.project.repository.ScheduledPaymentRepository;
import com.project.service.ScheduledPaymentService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ScheduledPaymentServiceImplementation implements ScheduledPaymentService {

	@Autowired
	ScheduledPaymentRepository repo;

	@Autowired
	BeneficiaryRepository beneficiaryRepo;

	@Autowired
	BankAccountRepository accountRepo;

	@Override
	public ScheduledPaymentDto createSchedule(ScheduledPaymentDto dto) throws Exception {
		if (dto == null) {
			throw new Exception("Scheduled payment details are required.");
		}
		if (dto.getCustomerId() == null) {
			throw new Exception("customerId is required.");
		}
		if (dto.getFromAccountId() == null) {
			throw new Exception("fromAccountId is required.");
		}
		if (dto.getBeneficiaryId() == null) {
			throw new Exception("beneficiaryId is required.");
		}
		if (dto.getAmount() == null || dto.getAmount() <= 0) {
			throw new Exception("Scheduled payment amount must be greater than zero.");
		}
		if (dto.getFrequency() == null) {
			throw new Exception("frequency is required.");
		}
		if (dto.getNextRunDate() == null) {
			throw new Exception("nextRunDate is required.");
		}
		if (dto.getNextRunDate().isBefore(LocalDate.now())) {
			throw new Exception("nextRunDate cannot be in the past.");
		}

		BankAccount source = accountRepo.findById(dto.getFromAccountId())
				.orElseThrow(() -> new Exception("Source account not found with ID: " + dto.getFromAccountId()));
		if (!dto.getCustomerId().equals(source.getCustomerId())) {
			throw new Exception("Source account does not belong to this customer.");
		}
		if (source.getStatus() != AccountStatus.Active) {
			throw new Exception("Source account is not active.");
		}

		Beneficiary beneficiary = beneficiaryRepo.findById(dto.getBeneficiaryId())
				.orElseThrow(() -> new Exception("Beneficiary not found with ID: " + dto.getBeneficiaryId()));
		if (!dto.getCustomerId().equals(beneficiary.getCustomerId())) {
			throw new Exception("Beneficiary does not belong to this customer.");
		}
		if (beneficiary.getStatus() != BeneficiaryStatus.Active) {
			throw new Exception("Beneficiary is not active.");
		}

		ScheduledPayment e = ScheduledPayment.builder()
				.customerId(dto.getCustomerId())
				.fromAccountId(dto.getFromAccountId())
				.beneficiary(beneficiary)
				.amount(dto.getAmount())
				.frequency(dto.getFrequency())
				.nextRunDate(dto.getNextRunDate())
				.status(ScheduledPaymentStatus.Active)
				.build();
		ScheduledPayment res = repo.save(e);
		log.info("Scheduled payment {} created for customer {}", res.getScheduleId(), res.getCustomerId());
		return toDto(res);
	}

	@Override
	public List<ScheduledPaymentDto> getSchedulesByCustomer(Integer customerId) {
		List<ScheduledPaymentDto> dtos = new ArrayList<>();
		for (ScheduledPayment e : repo.findByCustomerId(customerId)) {
			dtos.add(toDto(e));
		}
		return dtos;
	}

	@Override
	public ScheduledPaymentDto pauseSchedule(Integer id) throws Exception {
		ScheduledPayment e = repo.findById(id).orElseThrow(() -> new Exception("ScheduledPayment not found with ID: " + id));
		if (e.getStatus() == ScheduledPaymentStatus.Cancelled) {
			throw new Exception("Cancelled scheduled payments cannot be paused.");
		}
		e.setStatus(ScheduledPaymentStatus.Paused);
		ScheduledPayment res = repo.save(e);
		log.info("Scheduled payment {} paused", id);
		return toDto(res);
	}

	@Override
	public ScheduledPaymentDto cancelSchedule(Integer id) throws Exception {
		ScheduledPayment e = repo.findById(id).orElseThrow(() -> new Exception("ScheduledPayment not found with ID: " + id));
		if (e.getStatus() == ScheduledPaymentStatus.Cancelled) {
			return toDto(e);
		}
		e.setStatus(ScheduledPaymentStatus.Cancelled);
		ScheduledPayment res = repo.save(e);
		log.info("Scheduled payment {} cancelled", id);
		return toDto(res);
	}

	@Override
	public List<ScheduledPaymentDto> getDuePayments(LocalDate date) {
		List<ScheduledPaymentDto> dtos = new ArrayList<>();
		for (ScheduledPayment e : repo.findByStatusAndNextRunDateLessThanEqual(ScheduledPaymentStatus.Active, date)) {
			dtos.add(toDto(e));
		}
		return dtos;
	}

	private ScheduledPaymentDto toDto(ScheduledPayment e) {
		ScheduledPaymentDto dto = new ScheduledPaymentDto();
		dto.setScheduleId(e.getScheduleId());
		dto.setCustomerId(e.getCustomerId());
		dto.setFromAccountId(e.getFromAccountId());
		dto.setBeneficiaryId(e.getBeneficiary() == null ? null : e.getBeneficiary().getBeneficiaryId());
		dto.setAmount(e.getAmount());
		dto.setFrequency(e.getFrequency());
		dto.setNextRunDate(e.getNextRunDate());
		dto.setStatus(e.getStatus());
		return dto;
	}
}