package com.bishop.application.service;

import com.bishop.application.dto.TransactionRequest;
import com.bishop.application.dto.TransactionResponse;
import com.bishop.application.enums.TransactionType;
import com.bishop.application.exception.CustomException;
import org.springframework.validation.BindingResult;

public interface RequestProcessorService {
    TransactionResponse processTransactionRequest(String rrn, TransactionRequest transactionRequest, BindingResult bindingResult, TransactionType type) throws CustomException;
}
