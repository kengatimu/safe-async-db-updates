package com.bishop.application.web.controller;

import com.bishop.application.dto.TransactionRequest;
import com.bishop.application.dto.TransactionResponse;
import com.bishop.application.enums.TransactionType;
import com.bishop.application.exception.CustomException;
import com.bishop.application.service.RequestProcessorService;
import com.bishop.application.service.TransactionUpdateService;
import com.google.gson.Gson;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

@RestController
@RequestMapping(path = "/api/v1")
@Tag(name = "Transaction API", description = "Handles credit transfer transactions. Returns HTTP 200 for success, HTTP 400/408/409 for different error conditions.")
public class TransactionController {
    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);

    private final Gson gson;
    private final RequestProcessorService requestProcessorService;
    private final TransactionUpdateService transactionUpdateService;

    @Autowired
    public TransactionController(Gson gson,
                                 RequestProcessorService requestProcessorService,
                                 TransactionUpdateService transactionUpdateService) {
        this.gson = gson;
        this.requestProcessorService = requestProcessorService;
        this.transactionUpdateService = transactionUpdateService;
    }

    // Swagger information
    @Operation(summary = "Process a credit transfer transaction", description = "Accepts a transaction request and processes it. " + "Returns HTTP 200 on success, or appropriate HTTP error codes on failure (400, 408, 409).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaction processed successfully"),
            @ApiResponse(responseCode = "400", description = "Internal Error: Validation failed or general processing error"),
            @ApiResponse(responseCode = "408", description = "Request timeout: No response from remote service"),
            @ApiResponse(responseCode = "409", description = "Conflict: Duplicate transaction detected")})

    @PostMapping(value = "/transaction",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> transaction(@Valid @RequestBody TransactionRequest transactionRequest,
                                         BindingResult bindingResult,
                                         WebRequest webRequest) throws CustomException {
        final TransactionType type = TransactionType.CREDIT_TRANSFER;
        final String rrn = transactionRequest.getRrn();

        // Store request details in WebRequest for error tracing
        webRequest.setAttribute("rrn", rrn, WebRequest.SCOPE_REQUEST);
        webRequest.setAttribute("type", type, WebRequest.SCOPE_REQUEST);

        try {
            // Log incoming request
            log.info("{}: Received {} Request From Channel: {}", rrn, type, gson.toJson(transactionRequest));

            // Process transaction request
            TransactionResponse response = requestProcessorService.processTransactionRequest(rrn, transactionRequest, bindingResult, type);

            // Update database asynchronously after processing
            updateDatabaseRecord(rrn, response, type);

            // Log outgoing response
            log.info("{}: Returned {} Response To Channel: {}", rrn, type, gson.toJson(response));

            return ResponseEntity.ok(gson.toJson(response));
        } catch (Exception e) {
            // Log and wrap any processing exceptions
            log.error("{}: Exception Occurred During Request Processing: {}", rrn, e.getMessage());
            throw new CustomException(e.getMessage());
        }
    }

    // Updates transaction status asynchronously in the database
    private void updateDatabaseRecord(String rrn, TransactionResponse response, TransactionType type) {
        transactionUpdateService.updateDbStatus(rrn, response, type);
    }
}
