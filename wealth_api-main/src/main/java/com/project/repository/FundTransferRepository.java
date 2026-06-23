package com.project.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.project.entities.FundTransfer;
import com.project.enums.TransferStatus;

@Repository
public interface FundTransferRepository extends JpaRepository<FundTransfer, Integer> {

	List<FundTransfer> findByFromAccountId(Integer fromAccountId);

	List<FundTransfer> findByToAccountId(Integer toAccountId);

	List<FundTransfer> findByStatus(TransferStatus status);

	List<FundTransfer> findByFromAccountIdOrToAccountId(Integer fromAccountId, Integer toAccountId);

	@Query("SELECT f FROM FundTransfer f WHERE (f.fromAccountId = :accountId OR f.toAccountId = :accountId) "
			+ "AND f.transferDate BETWEEN :from AND :to")
	List<FundTransfer> findStatement(@Param("accountId") Integer accountId, @Param("from") LocalDate from,
			@Param("to") LocalDate to);
}
