package com.bishop.application.service;

import com.bishop.application.dto.TransactionRequest;
import com.bishop.application.dto.TransactionResponse;
import com.bishop.application.entity.TransactionDetails;
import com.bishop.application.enums.TransactionType;

public interface TransactionMapperService {
    TransactionDetails mapRequestToEntity(TransactionRequest request, TransactionType type);

    TransactionDetails composeErrorStatusEntity(String rrn, TransactionResponse channelResponse, TransactionType type);
}
