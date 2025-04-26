package com.bishop.application.dto;

import java.io.Serializable;
import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "rrn",
        "transactionType",
        "senderName",
        "receiverName",
        "amount",
        "currency",
        "channelId"
})
public class TransactionRequest implements Serializable {
    private static final long serialVersionUID = -5923631116281590822L;

    @NotBlank(message = "RRN is required")
    @Size(min = 8, max = 20, message = "RRN must be between 8 and 20 characters")
    @Pattern(regexp = "^[A-Za-z0-9\\-]+$", message = "RRN must only contain letters, numbers, and dashes")
    @JsonProperty("rrn")
    @Schema(description = "Retrieval Reference Number", example = "TX99887766")
    private String rrn;

    @NotBlank(message = "Transaction type is required")
    @Pattern(regexp = "^[A-Za-z_-]+$", message = "Transaction Type must be letters and underscores only")
    @JsonProperty("transactionType")
    @Schema(description = "Type of transaction (e.g., transaction, payment)", example = "CREDIT-TRANSFER")
    private String transactionType;

    @NotBlank(message = "Sender name is required")
    @Size(max = 100, message = "Sender name must not exceed 100 characters")
    @Pattern(regexp = "^[A-Za-z0-9 .'-]+$", message = "Sender name contains invalid characters")
    @JsonProperty("senderName")
    @Schema(description = "Full name of the sender", example = "John Doe")
    private String senderName;

    @NotBlank(message = "Receiver name is required")
    @Size(max = 100, message = "Receiver name must not exceed 100 characters")
    @Pattern(regexp = "^[A-Za-z0-9 .'-]+$", message = "Receiver name contains invalid characters")
    @JsonProperty("receiverName")
    @Schema(description = "Full name of the receiver", example = "Jane Smith")
    private String receiverName;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Digits(integer = 18, fraction = 2, message = "Amount must be a valid number with up to two decimal places")
    @JsonProperty("amount")
    @Schema(description = "Transaction amount", example = "1500.00")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be uppercase 3-letter code")
    @JsonProperty("currency")
    @Schema(description = "Currency code (3-letter ISO 4217)", example = "KES")
    private String currency;

    @NotBlank(message = "Channel ID is required")
    @Size(max = 20, message = "Channel ID must not exceed 20 characters")
    @Pattern(regexp = "^[A-Za-z0-9_\\-]+$", message = "Channel ID must only contain letters, numbers, dashes, and underscores")
    @JsonProperty("channelId")
    @Schema(description = "Channel identifier", example = "OMNI")
    private String channelId;

    @JsonProperty("rrn")
    public String getRrn() {
        return rrn;
    }

    @JsonProperty("rrn")
    public void setRrn(String rrn) {
        this.rrn = rrn;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    @JsonProperty("senderName")
    public String getSenderName() {
        return senderName;
    }

    @JsonProperty("senderName")
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    @JsonProperty("receiverName")
    public String getReceiverName() {
        return receiverName;
    }

    @JsonProperty("receiverName")
    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    @JsonProperty("currency")
    public String getCurrency() {
        return currency;
    }

    @JsonProperty("currency")
    public void setCurrency(String currency) {
        this.currency = currency;
    }

    @JsonProperty("channelId")
    public String getChannelId() {
        return channelId;
    }

    @JsonProperty("channelId")
    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }
}