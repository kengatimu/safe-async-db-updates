package com.bishop.application.service;

import com.bishop.application.dto.TransactionResponse;
import com.bishop.application.enums.TransactionType;

public interface TransactionUpdateService {
    void updateDbStatus(String rrn, TransactionResponse channelResponse, TransactionType type);
}
