package com.foodexpress.common.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration utilities.
 * Provides standard configurations for producers and consumers.
 */
public final class KafkaConfigUtils {
    
    private KafkaConfigUtils() {} // Utility class
    
    // ========================================
    // TOPIC NAMES (Constants)
    // ========================================
    
    public static final class Topics {
        public static final String ORDER_CREATED = "foodexpress.order.created";
        public static final String ORDER_CONFIRMED = "foodexpress.order.confirmed";
        public static final String ORDER_CANCELLED = "foodexpress.order.cancelled";
        public static final String ORDER_STATUS_CHANGED = "foodexpress.order.status-changed";
        
        public static final String PAYMENT_INITIATED = "foodexpress.payment.initiated";
        public static final String PAYMENT_SUCCESS = "foodexpress.payment.success";
        public static final String PAYMENT_FAILED = "foodexpress.payment.failed";
        public static final String PAYMENT_REFUNDED = "foodexpress.payment.refunded";
        
        public static final String NOTIFICATION_REQUEST = "foodexpress.notification.request";
        public static final String NOTIFICATION_SENT = "foodexpress.notification.sent";
        
        public static final String AUDIT_LOG = "foodexpress.audit.log";
        public static final String USER_REGISTERED = "foodexpress.user.registered";
        public static final String USER_UPDATED = "foodexpress.user.updated";
        
        private Topics() {}
    }
    
    // ========================================
    // CONSUMER GROUPS
    // ========================================
    
    public static final class ConsumerGroups {
        public static final String ORDER_SERVICE = "foodexpress-order-service";
        public static final String PAYMENT_SERVICE = "foodexpress-payment-service";
        public static final String NOTIFICATION_SERVICE = "foodexpress-notification-service";
        public static final String ANALYTICS_SERVICE = "foodexpress-analytics-service";
        public static final String USER_SERVICE = "foodexpress-user-service";
        
        private ConsumerGroups() {}
    }
    
    // ========================================
    // PRODUCER CONFIGURATIONS
    // ========================================
    
    /**
     * Standard producer configuration with idempotence enabled.
     */
    public static Map<String, Object> producerConfigs(String bootstrapServers) {
        Map<String, Object> props = new HashMap<>();
        
        // Connection
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        
        // Serialization
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        // Reliability - Exactly-once semantics
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
        
        // Performance tuning
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384); // 16KB
        props.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432); // 32MB
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        
        // Timeouts
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);
        
        return props;
    }
    
    /**
     * Transactional producer configuration for exactly-once semantics.
     */
    public static Map<String, Object> transactionalProducerConfigs(
            String bootstrapServers, String transactionIdPrefix) {
        Map<String, Object> props = producerConfigs(bootstrapServers);
        
        // Transaction support
        props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, 
                transactionIdPrefix + "-" + java.util.UUID.randomUUID());
        
        return props;
    }
    
    // ========================================
    // CONSUMER CONFIGURATIONS
    // ========================================
    
    /**
     * Standard consumer configuration.
     */
    public static Map<String, Object> consumerConfigs(String bootstrapServers, String groupId) {
        Map<String, Object> props = new HashMap<>();
        
        // Connection
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        
        // Deserialization
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.foodexpress.*");
        
        // Offset management
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        
        // Read committed for exactly-once
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        
        // Performance
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
        
        // Session management
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);
        
        return props;
    }
    
    /**
     * Consumer configuration optimized for high-throughput.
     */
    public static Map<String, Object> highThroughputConsumerConfigs(
            String bootstrapServers, String groupId) {
        Map<String, Object> props = consumerConfigs(bootstrapServers, groupId);
        
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 50000);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1000);
        
        return props;
    }
    
    // ========================================
    // KAFKA HEADERS
    // ========================================
    
    public static final class Headers {
        public static final String CORRELATION_ID = "X-Correlation-ID";
        public static final String EVENT_TYPE = "X-Event-Type";
        public static final String SOURCE_SERVICE = "X-Source-Service";
        public static final String TIMESTAMP = "X-Timestamp";
        public static final String IDEMPOTENCY_KEY = "X-Idempotency-Key";
        
        private Headers() {}
    }
}
