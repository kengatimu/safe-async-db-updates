package com.bishop.application.service.impl;

import com.bishop.application.dto.TransactionResponse;
import com.bishop.application.entity.TransactionDetails;
import com.bishop.application.enums.TransactionType;
import com.bishop.application.service.DatabaseService;
import com.bishop.application.service.TransactionMapperService;
import com.bishop.application.service.TransactionUpdateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

@Service
public class TransactionUpdateServiceImpl implements TransactionUpdateService {
    private static final Logger log = LoggerFactory.getLogger(TransactionUpdateServiceImpl.class);

    private final TaskExecutor taskExecutor;
    private final DatabaseService databaseService;
    private final TransactionMapperService transactionMapperService;

    @Autowired
    public TransactionUpdateServiceImpl(@Qualifier("taskExecutor") TaskExecutor taskExecutor,
                                        DatabaseService databaseService,
                                        TransactionMapperService transactionMapperService) {
        this.taskExecutor = taskExecutor;
        this.databaseService = databaseService;
        this.transactionMapperService = transactionMapperService;
    }

    // No @Transactional here because database update runs asynchronously in a separate thread
    @Override
    public void updateDbStatus(String rrn, TransactionResponse channelResponse, TransactionType type) {
        taskExecutor.execute(() -> {
            try {
                // Compose transaction entity from channel response
                TransactionDetails entity = transactionMapperService.composeErrorStatusEntity(rrn, channelResponse, type);

                if (entity != null) {
                    log.info("{}: Updating Database Status For Record With RRN: {}", rrn, rrn);
                    databaseService.updateTransactionRecord(rrn, entity);
                } else {
                    log.warn("{}: TransactionDetails Entity is null or empty, skipping database update", rrn);
                }
            } catch (Exception e) {
                log.error("{}: Failed to update database record asynchronously for RRN: {}. Error: {}", rrn, rrn, e.getMessage());
            }
        });
    }
}