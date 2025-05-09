package com.bishop.application.config;

public final class ApplicationConstants {

    // Application errors
    public static final int DEFAULT_HTTP_STATUS_CODE = 400;
    public static final String FIELD_VALIDATION_ERROR = "400|Internal Error: Field Validation Failed. ";
    public static final String HTTP_ERROR = "400|Internal Error: HTTP Call To The URL Was Unsuccessful. ";
    public static final String DEFAULT_PROCESSING_FAILURE = "400|Internal Error: Could not process the request. ";
    public static final String DUPLICATE_RECORD = "409|De-Dup! The request is a duplicate and has already been processed.";
    public static final String TIMEOUT_ERROR = "408|Did not receive a response from remote service, possibly due to timeout. ";
    public static final String DEFAULT_DATABASE_ERROR = "400|Internal Database Error: Error occurred while saving transaction: ";
    public static final String DEFAULT_RESPONSE_PROCESSING_FAILURE = "400|Internal Error: Could not process the received response. ";
    public static final String HTTP_RESPONSE_LOG_TEMPLATE = "HTTP Response: \n STATUS CODE: %s\n STATUS MESSAGE: %s\n RESPONSE BODY STRING: %s\n";

    private ApplicationConstants() {
    }
}