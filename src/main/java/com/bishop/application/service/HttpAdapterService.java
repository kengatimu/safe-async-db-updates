package com.bishop.application.service;

import com.bishop.application.dto.TransactionRequest;
import com.bishop.application.dto.TransactionResponse;
import com.bishop.application.enums.TransactionType;
import com.bishop.application.exception.CustomException;

public interface HttpAdapterService {
    TransactionResponse sendHttpTransactionRequest(TransactionRequest transactionRequest, TransactionType type) throws CustomException;
}
