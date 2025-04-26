package com.bishop.application.service.impl;

import com.bishop.application.dto.TransactionRequest;
import com.bishop.application.dto.TransactionResponse;
import com.bishop.application.entity.TransactionDetails;
import com.bishop.application.enums.TransactionStatus;
import com.bishop.application.enums.TransactionType;
import com.bishop.application.service.DatabaseService;
import com.bishop.application.service.TransactionMapperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class TransactionMapperServiceImpl implements TransactionMapperService {
    private static final Logger log = LoggerFactory.getLogger(TransactionMapperServiceImpl.class);

    private final DatabaseService databaseService;

    @Autowired
    public TransactionMapperServiceImpl(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    @Override
    public TransactionDetails mapRequestToEntity(TransactionRequest request, TransactionType type) {
        TransactionDetails transaction = new TransactionDetails();
        transaction.setRrn(request.getRrn());
        transaction.setSenderName(request.getSenderName());
        transaction.setReceiverName(request.getReceiverName());
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency());
        transaction.setChannelId(request.getChannelId());
        transaction.setTransactionType(type);
        transaction.setStatus(TransactionStatus.INITIALIZED.name());
        transaction.setStatusCode(TransactionStatus.INITIALIZED.getCode());
        transaction.setStatusDesc(TransactionStatus.INITIALIZED.getDescription());
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());
        return transaction;
    }

    @Override
    public TransactionDetails composeErrorStatusEntity(String rrn, TransactionResponse channelResponse, TransactionType type) {
        // Fetch existing transaction record
        TransactionDetails transactionRecord = databaseService.getSavedRecord(rrn, type);

        if (transactionRecord != null) {
            // Update status fields from the response
            transactionRecord.setStatus(channelResponse.getStatus());
            transactionRecord.setStatusCode(channelResponse.getStatusCode());
            transactionRecord.setStatusDesc(channelResponse.getStatusDesc());
            return transactionRecord;
        } else {
            return null;
        }
    }
}
