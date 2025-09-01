package com.igot.cb.workallocation.util;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@Getter
@Setter
public class CbServerProperties {

    @Value("${redis.insights.index}")
    private int redisInsightIndex;

    @Value("${search.result.redis.ttl}")
    private long searchResultRedisTtl;

    @Value("${sb.api.key}")
    private String sbApiKey;

    @Value("${http.client.request.factory.timeout}")
    private int requestTimeoutMs;

    @Value("${http.pooling.client.cm.max.total.connections}")
    private int maxTotalConnections;

    @Value("${http.pooling.client.cm.default.max.per.route}")
    private int maxConnectionsPerRoute;

    @Value("${redis.pool.max.total}")
    private int redisPoolMaxTotal;

    @Value("${redis.pool.max.idle}")
    private int redisPoolMaxIdle;

    @Value("${redis.pool.min.idle}")
    private int redisPoolMinIdle;

    @Value("${redis.pool.max.wait}")
    private int redisPoolMaxWait;

    @Value("${redis.connection.timeout}")
    private long redisConnectionTimeout;

    @Value("${redis.timeout}")
    private String redisTimeout;

    @Value("${redis.host.name}")
    private String redisHostName;

    @Value("${redis.port}")
    private String redisPort;

    @Value("${redis.data.host.name}")
    private String redisDataHostName;

    @Value("${redis.data.port}")
    private String redisDataPort;

    @Value("${cb.redis.maxIdle}")
    private Integer redisMaxIdle;

    @Value("${cb.redis.maxTotal}")
    private Integer redisMaxTotal;

    @Value("${cb.redis.minIdle}")
    private Integer redisMinIdle;

    @Value("${cb.redis.testOnBorrow}")
    private Boolean redisTestOnBorrow;

    @Value("${cb.redis.testOnReturn}")
    private Boolean redisTestOnReturn;

    @Value("${cb.redis.testWhileIdle}")
    private Boolean redisTestWhileIdle;

    @Value("${cb.redis.minEvictableIdleTimeMillis}")
    private Long redisMinEvictableIdleTimeMillis;

    @Value("${cb.redis.timeBetweenEvictionRunsMillis}")
    private Long redisTimeBetweenEvictionRunsMillis;

    @Value("${cb.redis.numTestsPerEvictionRun}")
    private Integer redisNumTestsPerEvictionRun;

    @Value("${cb.redis.blockWhenExhausted}")
    private Boolean redisBlockWhenExhausted;

    @Value("${cb.data.index}")
    private int dataIndex;

    @Value("${cb.cache.ttl}")
    private int cacheTtl;

    @Value("${user.profile.index}")
    public String userProfileIndex;

    @Value("${workallocation.entity}")
    private String workAllocationEntity;

    @Value("${elastic.field.workallocation.json.path}")
    private String elasticWorkAllocationJsonPath;

    @Value("${workorder.entity}")
    private String workOrderEntity;

    @Value("${elastic.field.workorder.json.path}")
    private String elasticWorkOrderJsonPath;

}
