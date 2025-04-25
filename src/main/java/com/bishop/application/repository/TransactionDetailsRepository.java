package com.bishop.application.repository;


import com.bishop.application.entity.TransactionDetails;
import com.bishop.application.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface TransactionDetailsRepository extends JpaRepository<TransactionDetails, Long> {
    @Transactional(readOnly = true)
    Optional<TransactionDetails> findByRrnAndTransactionType(String rrn, TransactionType type);
}
