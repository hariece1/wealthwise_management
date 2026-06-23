package com.project.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.project.enums.Currency;
import com.project.enums.TransferType;
import com.project.enums.TransferStatus;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "fund_transfer")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundTransfer {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer transferId;

	private Integer fromAccountId;
	private Integer toAccountId;
	private Double amount;
	@Enumerated(EnumType.STRING)
	private Currency currency;
	@Enumerated(EnumType.STRING)
	private TransferType transferType;
	@Column(length = 500)
	private String remarks;
	private LocalDate transferDate;
	@Enumerated(EnumType.STRING)
	private TransferStatus status;
}
