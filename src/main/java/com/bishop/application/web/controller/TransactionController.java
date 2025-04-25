package com.bishop.application.web.controller;

import com.bishop.application.dto.TransactionRequest;
import com.bishop.application.dto.TransactionResponse;
import com.bishop.application.enums.TransactionType;
import com.bishop.application.exception.CustomException;
import com.bishop.application.service.RequestProcessorService;
import com.bishop.application.service.TransactionUpdateService;
import com.google.gson.Gson;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

    @PostMapping(value = "/transaction",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<?> transaction(@Valid @RequestBody TransactionRequest transactionRequest, BindingResult bindingResult, WebRequest webRequest) throws CustomException {
        TransactionType type = TransactionType.CREDIT_TRANSFER;
        String rrn = transactionRequest.getRrn();

        // Store the rrn and type in the WebRequest to be used in the GlobalExceptionHandler class
        webRequest.setAttribute("rrn", rrn, WebRequest.SCOPE_REQUEST);
        webRequest.setAttribute("type", type, WebRequest.SCOPE_REQUEST);

        try {
            log.info("{}: Received {} Request From Channel: {}", rrn, type, gson.toJson(transactionRequest));
            TransactionResponse response = requestProcessorService.processTransactionRequest(transactionRequest, bindingResult, type);

            // Update Db status for credit transfer only
            updateDatabaseRecord(rrn, response, type);

            log.info("{}: Returned {} Response To Channel: {}", rrn, type, gson.toJson(response));
            return new ResponseEntity<>(gson.toJson(response), HttpStatus.OK);
        } catch (Exception e) {
            log.error("{}: Exception Occurred During Request Processing: {}", rrn, e.getMessage());
            throw new CustomException(e.getMessage());
        }
    }

    // This will update the database in async (background thread). Therefore, not need for @Transaction annotation
    private void updateDatabaseRecord(String rrn, TransactionResponse response, TransactionType type) {
        transactionUpdateService.updateDbStatus(rrn, response, type);
    }
}
