package kh.com.cellcard.common.custom;

import kh.com.cellcard.common.constant.HttpHeaderConstant;
import kh.com.cellcard.service.ratelimiter.RateLimiterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class IpKeyResolver implements KeyResolver {

    private final JwtDecoder jwtDecoder;

    @Autowired
    private RateLimiterService rateLimiterService;

    public IpKeyResolver(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }


    /*@Override
    public Mono<String> resolve(ServerWebExchange exchange) {

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.split(" ")[1];
            try {
                var jwt = jwtDecoder.decode(token);
                String userId = jwt.getClaimAsString("sub"); // Extract user ID from the JWT
                return ratingRequest(userId, exchange); // Return user ID as the key
            } catch (Exception e) {
                System.err.println("Invalid JWT: " + e.getMessage());
            }
        }

        return Mono.just("anonymous");
    }*/

    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        return Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst(HttpHeaderConstant.AUTHORIZATION))
            .filter(authHeader -> authHeader.startsWith("Bearer"))
            .map(authHeader -> authHeader.substring(7))
            .flatMap(token -> Mono
                .fromCallable(() -> jwtDecoder.decode(token))
                //.map(jwt -> jwt.getClaimAsString("sub"))
                .flatMap(jwt -> ratingRequest(jwt, exchange))
                .onErrorResume(e -> {
                    System.err.println("Invalid JWT: " + e.getMessage());
                    return Mono.just("anonymous");
                })
            )
            .defaultIfEmpty("anonymous");
    }

    private Mono<String> ratingRequest(Jwt jwt, ServerWebExchange exchange) {

        var key = jwt.getClaimAsString("sub");
        return rateLimiterService.resolveBucket(jwt)
            .flatMap(bucket -> bucket.tryConsume(1)
                ? Mono.just(key)
                : buildThrottledResponse(exchange)
            );

        /*var bucket = rateLimiterService.resolveBucket(key);

        if (bucket.tryConsume(1)) {
            return Mono.just(key);
        }

        System.err.println("==> Error Throttled User Level, user : " + key);

        return buildThrottledResponse(exchange);*/
    }

    private Mono<String> buildThrottledResponse(ServerWebExchange exchange) {
        return Mono.defer(() -> {
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            String responseBody = "{\"error\": \"The request is throttled\"}";
            byte[] bytes = responseBody.getBytes();

            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                    .bufferFactory()
                    .wrap(bytes)))
                .then(Mono.empty());
        });
    }
}
