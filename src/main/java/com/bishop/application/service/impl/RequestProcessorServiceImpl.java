package com.bishop.application.service.impl;

import com.bishop.application.dto.TransactionRequest;
import com.bishop.application.dto.TransactionResponse;
import com.bishop.application.entity.TransactionDetails;
import com.bishop.application.enums.TransactionType;
import com.bishop.application.exception.CustomException;
import com.bishop.application.service.DatabaseService;
import com.bishop.application.service.HttpAdapterService;
import com.bishop.application.service.RequestProcessorService;
import com.bishop.application.service.TransactionMapperService;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;

import java.util.List;

import static com.bishop.application.config.ApplicationConstants.FIELD_VALIDATION_ERROR;

@Service
public class RequestProcessorServiceImpl implements RequestProcessorService {
    private static final Logger log = LoggerFactory.getLogger(RequestProcessorServiceImpl.class);

    private final DatabaseService databaseService;
    private final HttpAdapterService httpAdapterService;
    private final TransactionMapperService transactionMapperService;

    @Autowired
    public RequestProcessorServiceImpl(DatabaseService databaseService,
                                       @Qualifier("closeableHttpClient2") CloseableHttpClient closeableHttpClient,
                                       HttpAdapterService httpAdapterService,
                                       TransactionMapperService transactionMapperService) {
        this.databaseService = databaseService;
        this.httpAdapterService = httpAdapterService;
        this.transactionMapperService = transactionMapperService;
    }

    @Override
    public TransactionResponse processTransactionRequest(TransactionRequest transactionRequest, BindingResult bindingResult, TransactionType type) throws CustomException {
        String rrn = transactionRequest.getRrn();

        // Validate fields - When you include BindingResult, Spring does not throw the MethodArgumentNotValidException automatically.
        // We have to check for validation errors manually.
        checkForInputValidationErrors(bindingResult);

        // Check for duplicates
        checkTransactionExists(rrn, type);

        // Prepare Initial Db record
        TransactionDetails entity = transactionMapperService.mapRequestToEntity(transactionRequest, type);

        // Save the initial entity
        persistInitialEntity(entity, rrn);

        // Do http call to the remote service
        return httpAdapterService.sendHttpTransactionRequest(transactionRequest, type);
    }

    private void checkForInputValidationErrors(BindingResult bindingResult) throws CustomException {
        if (!bindingResult.hasErrors()) {
            return;
        }
        List<ObjectError> allErrors = bindingResult.getAllErrors();
        throw new CustomException(FIELD_VALIDATION_ERROR + allErrors.get(0).getDefaultMessage());
    }

    @Transactional(readOnly = true)
    private void checkTransactionExists(String rrn, TransactionType type) throws CustomException {
        log.info("{}: Checking transaction by RRN: {} and type: {}", rrn, rrn, type);
        databaseService.checkTransactionExists(rrn, type);
    }

    @Transactional
    private void persistInitialEntity(TransactionDetails entity, String rrn) throws CustomException {
        try {
            log.info("{}: Saving the initial credit transfer record in the database", rrn);
            databaseService.saveInitialCreditTransferEntity(rrn, entity);
        } catch (Exception e) {
            throw new CustomException(e.getMessage());
        }
    }
}
