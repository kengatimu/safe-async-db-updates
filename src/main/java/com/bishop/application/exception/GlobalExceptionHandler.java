package com.bishop.application.exception;

import com.bishop.application.dto.TransactionResponse;
import com.bishop.application.enums.TransactionType;
import com.bishop.application.service.TransactionUpdateService;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

import static com.bishop.application.config.ApplicationConstants.*;
import static com.bishop.application.enums.TransactionStatus.FAILURE;
import static com.bishop.application.enums.TransactionStatus.TIMEOUT;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final Gson gson;
    private final TransactionUpdateService transactionUpdateService;

    @Autowired
    public GlobalExceptionHandler(Gson gson,
                                  TransactionUpdateService transactionUpdateService) {
        this.gson = gson;
        this.transactionUpdateService = transactionUpdateService;
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<?> handleCustomExceptions(CustomException e, WebRequest request) {
        String rrn = (String) request.getAttribute("rrn", WebRequest.SCOPE_REQUEST);
        TransactionType type = (TransactionType) request.getAttribute("type", WebRequest.SCOPE_REQUEST);

        // Parse error code and error message from the exception
        Map<String, String> errorMap = getErrorDesc(e.getMessage());
        String errorMessage = errorMap.getOrDefault("message", "An unexpected error occurred");
        String httpStatusCode = errorMap.getOrDefault("code", String.valueOf(DEFAULT_HTTP_STATUS_CODE));

        // Validate HTTP status code (must be between 100-599)
        if ("0".equals(httpStatusCode) || httpStatusCode.isBlank()) {
            httpStatusCode = String.valueOf(DEFAULT_HTTP_STATUS_CODE);
        }
        if (Integer.parseInt(httpStatusCode) < 100 || Integer.parseInt(httpStatusCode) > 599) {
            httpStatusCode = String.valueOf(DEFAULT_HTTP_STATUS_CODE);
        }

        // Generate TransactionResponse object for the failure
        TransactionResponse response = generateResponse(errorMessage, rrn);

        // Update database asynchronously with failed status
        updateDatabaseRecord(rrn, response, type);

        // Log and return the error response
        log.info("{}: Returned {} Response To Channel: {}", rrn, type, gson.toJson(response));
        return new ResponseEntity<>(response, HttpStatus.valueOf(Integer.parseInt(httpStatusCode)));
    }

    // No @Transactional here because database update is handled asynchronously in background thread
    private void updateDatabaseRecord(String rrn, TransactionResponse response, TransactionType type) {
        transactionUpdateService.updateDbStatus(rrn, response, type);
    }

    // Create a failure response based on error type
    private TransactionResponse generateResponse(String errorMessage, String rrn) {
        String errorCode = String.valueOf(FAILURE.getCode());
        String errorStatus = FAILURE.name();

        // Special handling for timeout errors
        if (errorMessage.contains("timeout")
                || errorMessage.contains("time out")
                || errorMessage.contains("timed out")) {
            errorCode = String.valueOf(TIMEOUT.getCode());
            errorStatus = TIMEOUT.name();
        }

        TransactionResponse transactionResponse = new TransactionResponse();
        transactionResponse.setStatus(errorStatus);
        transactionResponse.setStatusCode(errorCode);
        transactionResponse.setStatusDesc(errorMessage);

        return transactionResponse;
    }

    // Parse the error string into a map with code and message parts
    private Map<String, String> getErrorDesc(String errorMessage) {
        Map<String, String> errorMap = new HashMap<>();

        if (errorMessage == null || errorMessage.isEmpty()) {
            errorMessage = DEFAULT_PROCESSING_FAILURE;
        }

        String[] parts = errorMessage.split("\\|", 2); // Split into two parts: code and message

        if (parts.length == 2) {
            errorMap.put("code", parts[0]);
            errorMap.put("message", parts[1]);
        } else {
            errorMap.put("code", "400");
            errorMap.put("message", "Internal Error: Could not process the request");
        }
        return errorMap;
    }
}
