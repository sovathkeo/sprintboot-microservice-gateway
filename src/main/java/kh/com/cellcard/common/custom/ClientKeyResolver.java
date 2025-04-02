package kh.com.cellcard.common.custom;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import kh.com.cellcard.common.configuration.appsetting.ApplicationConfiguration;
import kh.com.cellcard.common.constant.HttpHeaderConstant;
import kh.com.cellcard.common.helper.JsonObjectHelper;
import kh.com.cellcard.common.helper.JwtHelper;
import kh.com.cellcard.common.helper.StringHelper;
import kh.com.cellcard.common.wrapper.SerializationWrapper;
import kh.com.cellcard.model.auth.AuthorizeResultModel;
import kh.com.cellcard.model.response.Response;
import kh.com.cellcard.service.ratelimiter.RateLimiterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Slf4j
@Component
public class ClientKeyResolver implements KeyResolver {

    private final ApplicationConfiguration appSetting;
    private final ReactiveJwtDecoder jwtDecoder;
    private final RateLimiterService rateLimiterService;
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    private final NimbusJwtEncoder jwtEncoder;

    public ClientKeyResolver(
        ApplicationConfiguration appSetting,
        ReactiveJwtDecoder jwtDecoder,
        RateLimiterService rateLimiterService,
        ReactiveRedisTemplate<String, String> redisTemplate,
        NimbusJwtEncoder jwtEncoder) {
        this.appSetting = appSetting;

        this.redisTemplate = redisTemplate;
        this.jwtDecoder = jwtDecoder;
        this.rateLimiterService = rateLimiterService;
        this.jwtEncoder = jwtEncoder;
    }

    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        return Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst(HttpHeaderConstant.AUTHORIZATION))
            .filter(authHeader -> authHeader.startsWith("Bearer"))
            .map(authHeader -> authHeader.substring(7))
            .flatMap(token -> decodeJwt(token)
                .flatMap(jwt -> authorize(jwt, exchange))
                .flatMap(authResult -> {
                    if (authResult.isAuthorized) {
                        return ratingRequest(authResult.jwt, exchange);
                    }
                    return buildUnAuthorizedResponse(exchange, authResult.errorMessage);
                })
                .onErrorResume(e -> {
                    log.error("Invalid JWT: {}", e.getMessage(), e);
                    return Mono.just("anonymous");
                })
            )
            .defaultIfEmpty("anonymous");
    }

    private Mono<AuthorizeResultModel> authorize(Jwt jwt, ServerWebExchange exchange) {
        var sub = jwt.getClaimAsString("sub");
        var key = "auth:client:" + sub;
        if (StringHelper.isNullOrEmpty(sub)) {
            // If claim sub is empty, so bypass validate permission
            log.debug("sub claim is empty. so bypass validation permission");
            return Mono.just(AuthorizeResultModel.success(jwt));
        }

        var uri = exchange.getRequest().getURI().getRawPath();

        return redisTemplate.opsForValue().get(key)
            .flatMap(cachedValue -> {
                var json = SerializationWrapper.deserialize(cachedValue, JsonObject.class);
                var clientId = JsonObjectHelper.getAsStringOrEmpty(json, "clientId");

                if (!Objects.equals(clientId, sub)) {
                    return Mono.just(AuthorizeResultModel.unAuthorized("client id miss-match"));
                }

                var resources = JsonObjectHelper.getAsJsonArray(json, "resources");

                if (!isAllowedAccessUrl(resources, uri)) {
                    return Mono.just(AuthorizeResultModel.unAuthorized("you are not authorized to access the resource"));
                }

                return isAllowedAccessProvisioningSpecification(resources, exchange)
                    .map(isAllowed -> isAllowed
                        ? AuthorizeResultModel.success(jwt)
                        : AuthorizeResultModel.unAuthorized("you are not authorized to access this specification")
                    );
            });
    }

    private boolean isAllowedAccessUrl(JsonArray jsonArrayResource, String url) {
        var isValidJson = !jsonArrayResource.isEmpty() && !jsonArrayResource.isJsonNull();
        return isValidJson && jsonArrayResource
            .asList()
            .stream()
            .anyMatch(e -> {
                var resourceNameCached = JsonObjectHelper.getAsStringOrEmpty(e, "name");
                return url.contains(resourceNameCached);
            });
    }

    private Mono<Boolean> isAllowedAccessProvisioningSpecification(JsonArray resourcesCached, ServerWebExchange exchange) {
        return exchange.getRequest().getBody()
            .flatMap(dataBuffer -> {

                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(bytes);
                String requestBody = new String(bytes);
                var bodyJson = SerializationWrapper.deserialize(requestBody, JsonObject.class);
                var requestSpecification = JsonObjectHelper.getAsStringOrEmpty(bodyJson, "service_specification.name");

                if (StringHelper.isNullOrEmpty(requestSpecification)) {
                    return Mono.just(false);
                }

                var isRequestSpecificationAllowed  = resourcesCached.asList().stream()
                    .anyMatch(r -> {
                        var resourceNameCached = JsonObjectHelper.getAsStringOrEmpty(r, "name");
                        if (!resourceNameCached.contains(":")) {
                            return false;
                        }
                        var specification = resourceNameCached.split(":")[1];
                        return specification.equalsIgnoreCase(requestSpecification);
                    });
                return Mono.just(isRequestSpecificationAllowed);
            }).next();
    }

    private Mono<Jwt> decodeJwt(String token) {
        var jwtDecoded = JwtHelper.decode(token);
        if (jwtDecoded == null) {
            return Mono.error(new RuntimeException("Error decoding token"));
        }
        var issuer = jwtDecoded.getOrDefault("iss", "");
        if (appSetting.authentication.isInternalAuthServer(issuer)) {
            return jwtDecoder.decode(token);
        }
        return Mono.just(jwtEncoder.encode(JwtEncoderParameters.from(JwtClaimsSet.builder()
            .subject(jwtDecoded.getOrDefault("sub", ""))
            .build())));

    }

    private Mono<String> ratingRequest(Jwt jwt, ServerWebExchange exchange) {

        var key = jwt.getClaimAsString("sub");
        return rateLimiterService.resolveBucket(jwt)
            .flatMap(bucket -> bucket.tryConsume(1)
                ? Mono.just(key)
                : buildThrottledResponse(exchange)
            );
    }

    private Mono<String> buildThrottledResponse(ServerWebExchange exchange) {
        return Mono.defer(() -> {
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            var responseBody = Response.failure("429", "The request is throttled", "Request reached Quota");
            byte[] bytes = responseBody.getBytes();

            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                    .bufferFactory()
                    .wrap(bytes)))
                .then(Mono.empty());
        });
    }

    private Mono<String> buildUnAuthorizedResponse(ServerWebExchange exchange, String errorMessage) {
        return Mono.defer(() -> {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            var responseBody = Response.failure("401", "You're not authorized", errorMessage);
            byte[] bytes = responseBody.getBytes();

            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                    .bufferFactory()
                    .wrap(bytes)))
                .then(Mono.empty());
        });
    }
}