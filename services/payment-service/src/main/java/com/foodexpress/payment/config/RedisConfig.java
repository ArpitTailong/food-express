package com.foodexpress.payment.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Redis/Redisson configuration for Payment Service.
 * 
 * Used for:
 * - Idempotency key storage
 * - Distributed locking
 * - Caching
 * 
 * Disabled when spring.data.redis.repositories.enabled=false
 */
@Configuration
@ConditionalOnProperty(name = "spring.data.redis.repositories.enabled", havingValue = "true", matchIfMissing = true)
public class RedisConfig {
    
    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;
    
    @Value("${spring.data.redis.port:6379}")
    private int redisPort;
    
    @Value("${spring.data.redis.password:}")
    private String redisPassword;
    
    @Value("${spring.data.redis.database:0}")
    private int redisDatabase;
    
    /**
     * Single node Redis configuration (development/single instance)
     */
    @Bean
    @Profile("!cluster")
    public RedissonClient redissonClient() {
        Config config = new Config();
        
        String address = "redis://" + redisHost + ":" + redisPort;
        
        config.useSingleServer()
                .setAddress(address)
                .setDatabase(redisDatabase)
                .setConnectionMinimumIdleSize(5)
                .setConnectionPoolSize(50)
                .setConnectTimeout(10000)
                .setTimeout(3000)
                .setRetryAttempts(3)
                .setRetryInterval(1500);
        
        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.useSingleServer().setPassword(redisPassword);
        }
        
        // Use JSON codec for better debugging
        config.setCodec(new JsonJacksonCodec());
        
        return Redisson.create(config);
    }
    
    /**
     * Redis Cluster configuration (production)
     */
    @Bean
    @Profile("cluster")
    public RedissonClient redissonClusterClient(
            @Value("${spring.data.redis.cluster.nodes:}") String clusterNodes) {
        
        Config config = new Config();
        
        String[] nodes = clusterNodes.split(",");
        String[] nodeAddresses = new String[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            nodeAddresses[i] = "redis://" + nodes[i].trim();
        }
        
        config.useClusterServers()
                .addNodeAddress(nodeAddresses)
                .setMasterConnectionMinimumIdleSize(5)
                .setMasterConnectionPoolSize(50)
                .setSlaveConnectionMinimumIdleSize(5)
                .setSlaveConnectionPoolSize(50)
                .setConnectTimeout(10000)
                .setTimeout(3000)
                .setRetryAttempts(3)
                .setRetryInterval(1500);
        
        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.useClusterServers().setPassword(redisPassword);
        }
        
        config.setCodec(new JsonJacksonCodec());
        
        return Redisson.create(config);
    }
}
