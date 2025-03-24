package kh.com.cellcard.service.ratelimiter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import jakarta.annotation.PostConstruct;
import kh.com.cellcard.common.helper.NumberHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Autowired
    private RedisClient redisClient;

    @Autowired
    private StatefulRedisConnection<String, byte[]> statefulRedisConnection;

    private LettuceBasedProxyManager<String> proxyManager;

    @PostConstruct
    public void init() {
        proxyManager = LettuceBasedProxyManager.builderFor(statefulRedisConnection)
            .build();
    }

    public Mono<Bucket> resolveBucket(Jwt jwt) {
        var key = jwt.getClaimAsString("sub");
        return checkRedisConnection()
            .flatMap(isHealthy -> isHealthy
                ? resolveBucketASync(jwt)
                : Mono.fromCallable(() -> buckets.computeIfAbsent(key, this::createBucket))
            );
    }

    public Bucket resolveBucketSync(String key) {

        BucketConfiguration bucketConfiguration = BucketConfiguration.builder()
            .addLimit(Bandwidth.builder()
                .capacity(1)
                .refillIntervally(1, Duration.ofSeconds(1))
                .build()
            )
            .build();
        RemoteBucketBuilder<String> builder = proxyManager.builder();

        var b = builder.build(key, () -> bucketConfiguration);

        System.out.printf("==> user[%s] available-token[%s]%n", key, b.getAvailableTokens());

        return b;
    }

    public Mono<Bucket> resolveBucketASync(Jwt jwt) {

        var key = jwt.getClaimAsString("sub");
        var tps = NumberHelper.toIntOrDefault(jwt.getClaimAsString("client_tps"), 1);

        BucketConfiguration bucketConfiguration = BucketConfiguration.builder()
            .addLimit(Bandwidth.builder()
                .capacity(tps)
                .refillIntervally(tps, Duration.ofSeconds(1))
                .build()
            )
            .build();
        RemoteBucketBuilder<String> builder = proxyManager.builder();

        var b = builder.build(key, () -> bucketConfiguration);

        System.out.printf("==> user[%s] available-token[%s], tps[%s]%n", key, b.getAvailableTokens(), tps);

        return Mono.just(b);
    }

    public Bucket createBucket(String key) {
        var limit = Bandwidth.builder()
            .capacity(1)
            .refillIntervally(1, Duration.ofSeconds(1))
            .build();

        return Bucket.builder().addLimit(limit).build();
    }

    public Mono<Boolean> checkRedisConnection() {
        return Mono.fromCallable(() -> {
            try(var connection = redisClient.connect()) {
                return connection.sync()
                    .ping()
                    .equalsIgnoreCase("PONG");
            }
        });
        /*return Mono.using(
            () -> redisClient.connect(),
            connection -> connection.reactive()
                .ping()
                .flatMap(res -> "PONG".equalsIgnoreCase(res) ? Mono.just(true) : Mono.just(false)),
            StatefulRedisConnection::close
        );*/
    }
}
