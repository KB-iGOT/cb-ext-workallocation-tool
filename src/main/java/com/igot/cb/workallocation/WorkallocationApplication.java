package com.igot.cb.workallocation;

import com.igot.cb.workallocation.util.CbServerProperties;
import com.igot.cb.workallocation.util.PropertiesCache;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@ComponentScan(basePackages = {"com.igot.cb.workallocation"})
@EntityScan("com.igot.cb.workallocation")
public class WorkallocationApplication {

    private final CbServerProperties serverProperties;

    public WorkallocationApplication(CbServerProperties serverProperties) {
        this.serverProperties = serverProperties;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate(getClientHttpRequestFactory());
    }

    @Bean
    public PropertiesCache propertiesCache() {
        return PropertiesCache.getInstance();
    }

    public static void main(String[] args) {
        SpringApplication.run(WorkallocationApplication.class, args);
    }

    private ClientHttpRequestFactory getClientHttpRequestFactory() {
        int timeout = serverProperties.getRequestTimeoutMs();
        RequestConfig config = RequestConfig.custom()
                .setResponseTimeout(Timeout.ofMilliseconds(timeout))
                .build();
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(serverProperties.getMaxTotalConnections());
        cm.setDefaultMaxPerRoute(serverProperties.getMaxConnectionsPerRoute());
        CloseableHttpClient client = HttpClients.custom()
                .setDefaultRequestConfig(config)
                .setConnectionManager(cm)
                .build();
        return new HttpComponentsClientHttpRequestFactory(client);
    }
}
