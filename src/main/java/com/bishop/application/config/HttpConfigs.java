package com.bishop.application.config;

import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.util.concurrent.TimeUnit;

@Configuration
public class HttpConfigs {

    private static final Logger log = LoggerFactory.getLogger(HttpConfigs.class);

    private static final int CONNECT_TIMEOUT = 15000;
    private static final int REQUEST_TIMEOUT = 15000;
    private static final int SOCKET_TIMEOUT = 30000;
    private static final int MAX_TOTAL_CONNECTIONS = 200;
    private static final int DEFAULT_MAX_PER_ROUTE = 50;
    private static final int DEFAULT_KEEP_ALIVE_TIME_MILLIS = 30000;
    private static final int CLOSE_IDLE_CONNECTION_WAIT_TIME_SECS = 30;

    private final String keystoreLocation;
    private final String keystorePassword;
    private final String keystoreType;
    private final boolean skipHostnameVerification;

    public HttpConfigs(@Value("${server.ssl.key-store}") String keystoreLocation,
                       @Value("${server.ssl.key-store-password}") String keystorePassword,
                       @Value("${server.ssl.key-store-type}") String keystoreType,
                       @Value("${http.ssl.skip-hostname-verification:false}") boolean skipHostnameVerification) {

        this.keystoreLocation = keystoreLocation;
        this.keystorePassword = keystorePassword;
        this.keystoreType = keystoreType;
        this.skipHostnameVerification = skipHostnameVerification;
    }

    @Bean("connectionManagerForEnabledHostnameVerification")
    public PoolingHttpClientConnectionManager connectionManagerForEnabledHostnameVerification() {
        return buildConnectionManager(false);
    }

    @Bean("connectionManagerForDisabledHostnameVerification")
    public PoolingHttpClientConnectionManager connectionManagerForDisabledHostnameVerification() {
        return buildConnectionManager(true);
    }

    @Bean("closeableHttpClient1")
    public CloseableHttpClient closeableHttpClient1(
            PoolingHttpClientConnectionManager connectionManagerForEnabledHostnameVerification) {
        return buildHttpClient(connectionManagerForEnabledHostnameVerification);
    }

    @Bean("closeableHttpClient2")
    public CloseableHttpClient closeableHttpClient2(
            PoolingHttpClientConnectionManager connectionManagerForDisabledHostnameVerification) {
        return buildHttpClient(connectionManagerForDisabledHostnameVerification);
    }

    private PoolingHttpClientConnectionManager buildConnectionManager(boolean disableHostnameVerification) {
        FileInputStream keystoreFis = null;

        try {
            KeyStore keyStore = KeyStore.getInstance(keystoreType);
            keystoreFis = new FileInputStream(keystoreLocation);
            keyStore.load(keystoreFis, keystorePassword.toCharArray());

            SSLContext sslContext = SSLContexts.custom()
                    .loadKeyMaterial(keyStore, keystorePassword.toCharArray())
                    .build();

            SSLConnectionSocketFactory socketFactory;
            if (disableHostnameVerification || skipHostnameVerification) {
                socketFactory = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
                log.warn("Hostname verification DISABLED for this pool.");
            } else {
                socketFactory = new SSLConnectionSocketFactory(
                        sslContext,
                        new String[]{"TLSv1.2"},
                        null,
                        new DefaultHostnameVerifier());
                log.info("Hostname verification ENABLED for this pool.");
            }

            Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("https", socketFactory)
                    .register("http", PlainConnectionSocketFactory.INSTANCE)
                    .build();

            PoolingHttpClientConnectionManager pool = new PoolingHttpClientConnectionManager(registry);
            pool.setMaxTotal(MAX_TOTAL_CONNECTIONS);
            pool.setDefaultMaxPerRoute(DEFAULT_MAX_PER_ROUTE);
            return pool;

        } catch (Exception e) {
            log.error("Could not initialize SSL connection manager", e);
            return new PoolingHttpClientConnectionManager();  // fallback
        } finally {
            try {
                if (keystoreFis != null) keystoreFis.close();
            } catch (IOException e) {
                log.error("Could not close keystoreFis FileInputStream: " + e.getMessage());
            }
        }
    }

    private CloseableHttpClient buildHttpClient(PoolingHttpClientConnectionManager cm) {
        RequestConfig config = RequestConfig.custom()
                .setConnectionRequestTimeout(REQUEST_TIMEOUT)
                .setConnectTimeout(CONNECT_TIMEOUT)
                .setSocketTimeout(SOCKET_TIMEOUT)
                .build();

        return HttpClients.custom()
                .setDefaultRequestConfig(config)
                .setConnectionManager(cm)
                .setKeepAliveStrategy(connectionKeepAliveStrategy())
                .build();
    }

    @Bean
    public ConnectionKeepAliveStrategy connectionKeepAliveStrategy() {
        return (HttpResponse response, HttpContext context) -> {
            HeaderElementIterator it =
                    new BasicHeaderElementIterator(response.headerIterator(HTTP.CONN_KEEP_ALIVE));
            while (it.hasNext()) {
                HeaderElement header = it.nextElement();
                if ("timeout".equalsIgnoreCase(header.getName()) && header.getValue() != null) {
                    return Long.parseLong(header.getValue()) * 1000;
                }
            }
            return DEFAULT_KEEP_ALIVE_TIME_MILLIS;
        };
    }

    @Scheduled(fixedDelay = 20000)
    public void idleConnectionMonitor(
            PoolingHttpClientConnectionManager connectionManagerForEnabledHostnameVerification,
            PoolingHttpClientConnectionManager connectionManagerForDisabledHostnameVerification) {
        cleanIdleConnections(connectionManagerForEnabledHostnameVerification, "secureMgr");
        cleanIdleConnections(connectionManagerForDisabledHostnameVerification, "insecureMgr");
    }

    private void cleanIdleConnections(PoolingHttpClientConnectionManager manager, String name) {
        try {
            int leased = manager.getTotalStats().getLeased();
            int available = manager.getTotalStats().getAvailable();

            if (leased > 0 || available > 0) {
                log.info("Cleaning idle/expired connections in {}...", name);
                manager.closeExpiredConnections();
                manager.closeIdleConnections(CLOSE_IDLE_CONNECTION_WAIT_TIME_SECS, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.error("Failed to clean connections for {}: {}", name, e.getMessage());
        }
    }
}
