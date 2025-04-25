package com.bishop.application.service;

import com.bishop.application.entity.TransactionDetails;
import com.bishop.application.enums.TransactionType;
import com.bishop.application.exception.CustomException;

public interface DatabaseService {
    void checkTransactionExists(String rrn, TransactionType type) throws CustomException;

    void saveInitialCreditTransferEntity(String rrn, TransactionDetails entity) throws CustomException;

    TransactionDetails getSavedRecord(String rrn, TransactionType type);

    void updateTransactionRecord(String rrn, TransactionDetails entity);
}
