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

import static com.bishop.application.config.ApplicationConstants.DEFAULT_HTTP_STATUS_CODE;
import static com.bishop.application.config.ApplicationConstants.DEFAULT_PROCESSING_FAILURE;
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

        // Parse error code and message from exception
        Map<String, String> errorMap = getErrorDesc(e.getMessage());
        String errorMessage = errorMap.getOrDefault("message", "An unexpected error occurred");
        String httpStatusCode = errorMap.getOrDefault("code", String.valueOf(DEFAULT_HTTP_STATUS_CODE));

        if ("0".equals(httpStatusCode) || httpStatusCode.isBlank()) {
            httpStatusCode = String.valueOf(DEFAULT_HTTP_STATUS_CODE);
        }

        // Check if the HTTP status code is valid (within range 100-599)
        if (Integer.parseInt(httpStatusCode) < 100 || Integer.parseInt(httpStatusCode) > 599) {
            httpStatusCode = String.valueOf(DEFAULT_HTTP_STATUS_CODE);
        }

        // Construct the response
        TransactionResponse response = generateResponse(errorMessage, rrn);

        // Update Db status for credit transfer only
        updateDatabaseRecord(rrn, response, type);

        log.info("{}: Returned {} Response To Channel: {}", rrn, type, gson.toJson(response));
        return new ResponseEntity<>(response, HttpStatus.valueOf(Integer.parseInt(httpStatusCode)));
    }

    // This will update the database in async (background thread). Therefore, not need for @Transaction annotation
    private void updateDatabaseRecord(String rrn, TransactionResponse response, TransactionType type) {
        transactionUpdateService.updateDbStatus(rrn, response, type);
    }

    private TransactionResponse generateResponse(String errorMessage, String rrn) {
        String errorCode = String.valueOf(FAILURE.getCode());
        String errorStatus = FAILURE.name();

        // Update error code for timeout and authorization
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

    private Map<String, String> getErrorDesc(String errorMessage) {
        Map<String, String> errorMap = new HashMap<>();

        if (errorMessage == null || errorMessage.isEmpty()) {
            errorMessage = DEFAULT_PROCESSING_FAILURE;
        }

        String[] parts = errorMessage.split("\\|", 2); // Limit to 2 parts

        if (parts.length == 2) {
            errorMap.put("code", parts[0]);
            errorMap.put("message", parts[1]);
        } else {
            // Fallback if format is not as expected
            errorMap.put("code", "400");
            errorMap.put("message", "Internal Error: Could not process the request");
        }
        return errorMap;
    }
}
