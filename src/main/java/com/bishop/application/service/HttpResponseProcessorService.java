package com.bishop.application.service;

import com.bishop.application.dto.TransactionResponse;
import com.bishop.application.enums.TransactionType;
import com.bishop.application.exception.CustomException;
import org.apache.http.client.methods.CloseableHttpResponse;

public interface HttpResponseProcessorService {
    TransactionResponse processTransactionResponse(String rrn, CloseableHttpResponse response, TransactionType type) throws CustomException;
}
