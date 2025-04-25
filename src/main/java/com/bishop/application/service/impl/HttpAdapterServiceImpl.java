package com.bishop.application.service.impl;

import com.bishop.application.dto.TransactionRequest;
import com.bishop.application.dto.TransactionResponse;
import com.bishop.application.enums.TransactionType;
import com.bishop.application.exception.CustomException;
import com.bishop.application.service.HttpAdapterService;
import com.bishop.application.service.HttpResponseProcessorService;
import com.google.gson.Gson;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;

import static com.bishop.application.config.ApplicationConstants.*;

@Service
public class HttpAdapterServiceImpl implements HttpAdapterService {
    private static final Logger log = LoggerFactory.getLogger(HttpAdapterServiceImpl.class);

    private final Gson gson;
    private final String transactionUrl;
    private final CloseableHttpClient closeableHttpClient;
    private final HttpResponseProcessorService httpResponseProcessorService;

    @Autowired
    public HttpAdapterServiceImpl(Gson gson,
                                  @Value("${urls.transaction}") String transactionUrl,
                                  @Qualifier("closeableHttpClient2") CloseableHttpClient closeableHttpClient,
                                  HttpResponseProcessorService httpResponseProcessorService) {
        this.gson = gson;
        this.transactionUrl = transactionUrl;
        this.closeableHttpClient = closeableHttpClient;
        this.httpResponseProcessorService = httpResponseProcessorService;
    }

    @Override
    public TransactionResponse sendHttpTransactionRequest(TransactionRequest transactionRequest, TransactionType type) throws CustomException {
        String rrn = transactionRequest.getRrn();
        CloseableHttpResponse response = null;
        try {
            HttpPost httpPost = getPostRequestHeaders(transactionUrl, new StringEntity(gson.toJson(transactionRequest)));
            log.info("{}: Sending {} Post HTTP Request Via: {}", rrn, type, transactionUrl);
            response = closeableHttpClient.execute(httpPost);

            return httpResponseProcessorService.processTransactionResponse(rrn, response, type);
        } catch (CustomException | IOException e) {
            log.error("{}: Exception Occurred on when sending http request: {}", rrn, e.getMessage());

            String errorDescription = e.getMessage();
            if (errorDescription != null && (
                    errorDescription.contains("timeout")
                            || errorDescription.contains("time out")
                            || errorDescription.contains("timed out"))) {
                throw new CustomException(TIMEOUT_ERROR + e.getMessage());
            }
            throw new CustomException(HTTP_ERROR + e.getMessage());
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    log.error("{}: Failed to close CloseableHttpResponse: {}", rrn, e.getMessage());
                }
            }
        }
    }

    private HttpPost getPostRequestHeaders(String url, StringEntity stringEntity) {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(stringEntity);
        httpPost.setHeader("Content-Type", MediaType.APPLICATION_XML_VALUE);
        return httpPost;
    }
}
