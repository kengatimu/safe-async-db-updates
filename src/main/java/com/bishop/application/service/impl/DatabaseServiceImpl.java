package com.bishop.application.service.impl;

import com.bishop.application.entity.TransactionDetails;
import com.bishop.application.enums.TransactionType;
import com.bishop.application.exception.CustomException;
import com.bishop.application.repository.TransactionDetailsRepository;
import com.bishop.application.service.DatabaseService;
import jakarta.persistence.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

import static com.bishop.application.config.ApplicationConstants.*;

@Service
public class DatabaseServiceImpl implements DatabaseService {
    private static final Logger log = LoggerFactory.getLogger(DatabaseServiceImpl.class);

    private final TransactionTemplate transactionTemplate;
    private final TransactionDetailsRepository transactionDetailsRepository;

    @Autowired
    public DatabaseServiceImpl(@Qualifier("Transactional") TransactionTemplate transactionTemplate,
                               @Lazy TransactionDetailsRepository transactionMasterRepository) {
        this.transactionTemplate = transactionTemplate;
        this.transactionDetailsRepository = transactionMasterRepository;
    }

    // Use @Transactional(readOnly = true) because this is a pure database read operation
    @Override
    @Transactional(readOnly = true)
    public void checkTransactionExists(String rrn, TransactionType type) throws CustomException {
        Optional<TransactionDetails> optional = transactionDetailsRepository.findByRrnAndTransactionType(rrn, type);

        if (optional.isPresent()) {
            log.error("{}: Duplicate transaction found for RRN: {}", rrn, rrn);
            throw new CustomException(DUPLICATE_RECORD);
        }
    }

    // Use @Transactional because we want the saveAndFlush operation to happen inside a database transaction
    @Override
    @Transactional
    public void saveInitialCreditTransferEntity(String rrn, TransactionDetails entity) throws CustomException {
        try {
            transactionDetailsRepository.saveAndFlush(entity);
            log.info("{}: Successfully persisted initial transaction record with RRN: {}", rrn, rrn);
        } catch (DataIntegrityViolationException e) {
            log.error("{}: Integrity violation when saving transaction entity: {}", rrn, e.getMessage());
            throw new CustomException(DEFAULT_DATABASE_ERROR + e.getMessage());
        } catch (PersistenceException | DataAccessException e) {
            log.error("{}: Persistence error occurred: {}", rrn, e.getMessage());
            throw new CustomException(DEFAULT_DATABASE_ERROR + e.getMessage());
        } catch (Exception e) {
            log.error("{}: Unexpected error occurred: {}", rrn, e.getMessage());
            throw new CustomException(DEFAULT_DATABASE_ERROR + e.getMessage());
        }
    }

    // Use @Transactional(readOnly = true) because we are fetching a record, no data is modified
    @Override
    @Transactional(readOnly = true)
    public TransactionDetails getSavedRecord(String rrn, TransactionType type) {
        Optional<TransactionDetails> optionalTransactionDetails = transactionDetailsRepository.findByRrnAndTransactionType(rrn, type);
        return optionalTransactionDetails.orElse(null);
    }

    // No @Transactional because this method runs inside an async thread (TransactionTemplate manually handles transaction here)
    @Override
    public void updateTransactionRecord(String rrn, TransactionDetails entity) {
        try {
            // Explicitly manage transaction boundary inside a background thread
            transactionTemplate.executeWithoutResult(status -> {
                transactionDetailsRepository.save(entity);
            });
            log.info("{}: Transaction updated successfully.", rrn);
        } catch (Exception e) {
            log.error("{}: Failed to update transaction record. Exception occurred: {}", rrn, e.getMessage());
        }
    }
}
