package com.muratsag.kafkaboard.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private final Map<String, BucketEntry> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, BucketEntry> registerBuckets = new ConcurrentHashMap<>();
    private final Map<String, BucketEntry> apiBuckets = new ConcurrentHashMap<>();

    public ConsumptionProbe consumeLoginRequest(String ipAddress) {
        return consume(loginBuckets, ipAddress, 10, Duration.ofMinutes(5));
    }

    public ConsumptionProbe consumeRegisterRequest(String ipAddress) {
        return consume(registerBuckets, ipAddress, 5, Duration.ofHours(1));
    }

    public ConsumptionProbe consumeApiRequest(String ipAddress) {
        return consume(apiBuckets, ipAddress, 60, Duration.ofMinutes(1));
    }

    @Scheduled(fixedDelay = 3600000)
    public void cleanupExpiredBuckets() {
        cleanupMap(loginBuckets, Duration.ofMinutes(10));
        cleanupMap(registerBuckets, Duration.ofHours(2));
        cleanupMap(apiBuckets, Duration.ofMinutes(5));
    }

    private ConsumptionProbe consume(
            Map<String, BucketEntry> cache,
            String ipAddress,
            long capacity,
            Duration refillDuration
    ) {
        BucketEntry entry = cache.compute(ipAddress, (key, existing) -> {
            if (existing == null) {
                return new BucketEntry(buildBucket(capacity, refillDuration), Instant.now());
            }
            existing.setLastAccessedAt(Instant.now());
            return existing;
        });

        return entry.getBucket().tryConsumeAndReturnRemaining(1);
    }

    private Bucket buildBucket(long capacity, Duration refillDuration) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(capacity, refillDuration)
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private void cleanupMap(Map<String, BucketEntry> cache, Duration ttl) {
        Instant cutoff = Instant.now().minus(ttl);
        cache.entrySet().removeIf(entry -> entry.getValue().getLastAccessedAt().isBefore(cutoff));
    }

    private static final class BucketEntry {
        private final Bucket bucket;
        private Instant lastAccessedAt;

        private BucketEntry(Bucket bucket, Instant lastAccessedAt) {
            this.bucket = bucket;
            this.lastAccessedAt = lastAccessedAt;
        }

        public Bucket getBucket() {
            return bucket;
        }

        public Instant getLastAccessedAt() {
            return lastAccessedAt;
        }

        public void setLastAccessedAt(Instant lastAccessedAt) {
            this.lastAccessedAt = lastAccessedAt;
        }
    }
}
