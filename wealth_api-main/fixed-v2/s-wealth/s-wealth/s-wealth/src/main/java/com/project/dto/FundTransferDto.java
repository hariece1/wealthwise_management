package com.project.dto;

import com.project.enums.Currency;
import com.project.enums.TransferType;
import com.project.enums.TransferStatus;

import java.time.LocalDate;

public class FundTransferDto {

	private Integer transferId;
	private Integer fromAccountId;
	private Integer toAccountId;
	private Double amount;
	private Currency currency;
	private TransferType transferType;
	private String remarks;
	private LocalDate transferDate;
	private TransferStatus status;

	public FundTransferDto() {}

	public FundTransferDto(Integer transferId, Integer fromAccountId, Integer toAccountId, Double amount, Currency currency, TransferType transferType, String remarks, LocalDate transferDate, TransferStatus status) {
		this.transferId = transferId;
		this.fromAccountId = fromAccountId;
		this.toAccountId = toAccountId;
		this.amount = amount;
		this.currency = currency;
		this.transferType = transferType;
		this.remarks = remarks;
		this.transferDate = transferDate;
		this.status = status;
	}

	public Integer getTransferId() { return transferId; }
	public void setTransferId(Integer transferId) { this.transferId = transferId; }

	public Integer getFromAccountId() { return fromAccountId; }
	public void setFromAccountId(Integer fromAccountId) { this.fromAccountId = fromAccountId; }

	public Integer getToAccountId() { return toAccountId; }
	public void setToAccountId(Integer toAccountId) { this.toAccountId = toAccountId; }

	public Double getAmount() { return amount; }
	public void setAmount(Double amount) { this.amount = amount; }

	public Currency getCurrency() { return currency; }
	public void setCurrency(Currency currency) { this.currency = currency; }

	public TransferType getTransferType() { return transferType; }
	public void setTransferType(TransferType transferType) { this.transferType = transferType; }

	public String getRemarks() { return remarks; }
	public void setRemarks(String remarks) { this.remarks = remarks; }

	public LocalDate getTransferDate() { return transferDate; }
	public void setTransferDate(LocalDate transferDate) { this.transferDate = transferDate; }

	public TransferStatus getStatus() { return status; }
	public void setStatus(TransferStatus status) { this.status = status; }
}
