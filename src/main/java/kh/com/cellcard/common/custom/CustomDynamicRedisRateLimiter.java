package kh.com.cellcard.common.custom;

import org.springframework.cloud.gateway.event.FilterArgsEvent;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class CustomDynamicRedisRateLimiter extends RedisRateLimiter {

    private final ReactiveJwtDecoder jwtDecoder;

    //private final StringRedisTemplate redisTemplate;

    public CustomDynamicRedisRateLimiter(ReactiveJwtDecoder jwtDecoder) {
        super(100, 150);
        this.jwtDecoder = jwtDecoder;
        //this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Response> isAllowed(String routeId, String id) {

        /*// request_rate_limiter.{admin}.replenish_rate
        // request_rate_limiter.{admin}.burst_capacity

        var replenishRate = redisTemplate.keys("request_rate_limiter.{admin}.replenish_rate")
            .stream()
            .findFirst()
            .orElse("0");
        var burstCapacity = redisTemplate.keys("request_rate_limiter.{admin}.burst_capacity")
            .stream()
            .findFirst()
            .orElse("0");

        var cfg = getConfig();*/

        return super.isAllowed(routeId, id);
    }

    @Override
    public Map<String, Config> getConfig() {
        return super.getConfig();
    }
}
