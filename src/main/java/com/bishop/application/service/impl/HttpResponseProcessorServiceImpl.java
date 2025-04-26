package com.bishop.application.service.impl;

import com.bishop.application.dto.TransactionResponse;
import com.bishop.application.enums.TransactionType;
import com.bishop.application.exception.CustomException;
import com.bishop.application.service.HttpResponseProcessorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

import static com.bishop.application.config.ApplicationConstants.*;

@Service
public class HttpResponseProcessorServiceImpl implements HttpResponseProcessorService {
    private static final Logger log = LoggerFactory.getLogger(HttpResponseProcessorServiceImpl.class);

    private final ObjectMapper objectMapper;

    @Autowired
    public HttpResponseProcessorServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public TransactionResponse processTransactionResponse(String rrn, CloseableHttpResponse response, TransactionType type) throws CustomException {
        try {
            // Extract HTTP entity and status details
            HttpEntity entity = response.getEntity();
            int statusCode = response.getStatusLine().getStatusCode();
            String httpStatusMsg = response.getStatusLine().getReasonPhrase();

            // Validate if response entity is empty or status is invalid
            if (entity == null || entity.getContentLength() == 0 || statusCode == 0) {
                throw new CustomException(TIMEOUT_ERROR);
            }

            // Convert response entity to String
            String responseString = EntityUtils.toString(entity);

            // Log the raw HTTP response
            log.info(String.format(HTTP_RESPONSE_LOG_TEMPLATE, statusCode, httpStatusMsg, responseString));

            // Map response JSON to TransactionResponse object
            return composeResponseObject(responseString);

        } catch (CustomException | IOException e) {
            throw new CustomException(DEFAULT_PROCESSING_FAILURE + e.getMessage());
        }
    }

    private TransactionResponse composeResponseObject(String responseString) throws CustomException {
        try {
            return objectMapper.readValue(responseString, TransactionResponse.class);
        } catch (Exception e) {
            log.error("Failed to parse TransactionResponse: {}", e.getMessage());
            throw new CustomException(DEFAULT_RESPONSE_PROCESSING_FAILURE);
        }
    }
}
