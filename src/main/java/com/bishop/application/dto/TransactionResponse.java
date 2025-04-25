
package com.bishop.application.dto;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "rrn",
    "status",
    "statusCode",
    "statusDesc",
    "tranId"
})
public class TransactionResponse implements Serializable {
    private final static long serialVersionUID = -2902798632027645166L;

    @JsonProperty("rrn")
    private String rrn;

    @JsonProperty("status")
    private String status;

    @JsonProperty("statusCode")
    private String statusCode;

    @JsonProperty("statusDesc")
    private String statusDesc;

    @JsonProperty("tranId")
    private String tranId;

    @JsonProperty("rrn")
    public String getRrn() {
        return rrn;
    }

    @JsonProperty("rrn")
    public void setRrn(String rrn) {
        this.rrn = rrn;
    }

    @JsonProperty("status")
    public String getStatus() {
        return status;
    }

    @JsonProperty("status")
    public void setStatus(String status) {
        this.status = status;
    }

    @JsonProperty("statusCode")
    public String getStatusCode() {
        return statusCode;
    }

    @JsonProperty("statusCode")
    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    @JsonProperty("statusDesc")
    public String getStatusDesc() {
        return statusDesc;
    }

    @JsonProperty("statusDesc")
    public void setStatusDesc(String statusDesc) {
        this.statusDesc = statusDesc;
    }

    @JsonProperty("tranId")
    public String getTranId() {
        return tranId;
    }

    @JsonProperty("tranId")
    public void setTranId(String tranId) {
        this.tranId = tranId;
    }

}
