package kh.com.cellcard.common.custom;

import com.google.gson.JsonObject;
import jakarta.annotation.Nonnull;
import kh.com.cellcard.common.configuration.appsetting.ApplicationConfiguration;
import kh.com.cellcard.common.helper.JsonObjectHelper;
import kh.com.cellcard.common.helper.JwtHelper;
import kh.com.cellcard.common.helper.StringHelper;
import kh.com.cellcard.common.wrapper.SerializationWrapper;
import kh.com.cellcard.common.wrapper.WebClientWrapper;
import kh.com.cellcard.model.auth.TokenIntrospectionResultModel;
import kh.com.cellcard.model.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;

/*
*  This custom authentication filter will do authenticate base on issuer in access token
* */

@Slf4j
@Component
public class CustomAuthenticationFilter implements WebFilter {

    private final ApplicationConfiguration appSetting;
    private final WebClientWrapper webClient;
    private final ReactiveJwtDecoder jwtDecoder;

    public CustomAuthenticationFilter(ApplicationConfiguration appSetting, WebClientWrapper webClient, ReactiveJwtDecoder jwtDecoder) {
        this.appSetting = appSetting;
        this.webClient = webClient;
        this.jwtDecoder = jwtDecoder;
    }

    @Nonnull
    @Override
    public Mono<Void> filter(ServerWebExchange exchange,@Nonnull  WebFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringHelper.isNullOrEmpty(authHeader) || !authHeader.toLowerCase().startsWith("bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            var responseBody = Response.failure("401", "Unauthorized", "Missing access token header");
            byte[] bytes = responseBody.getBytes();
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                    .bufferFactory()
                    .wrap(bytes)))
                .then(Mono.empty());
        }

        var token = authHeader.substring(7);

        return decodeJwt(token)
            .flatMap(jwtDecoded -> {
                if (!isInternalAuthServer(jwtDecoded)) {
                    var issuer = jwtDecoded.getOrDefault("iss", "");
                    return callToIntrospectEndpoint(issuer, token)
                        .flatMap(result ->
                            result.active
                                ? chain.filter(exchange)
                                : errorResponse(exchange, HttpStatus.UNAUTHORIZED,"401", "Unauthorized", "Token Invalid")
                        );
                }
                return validateTokenWithInternalAuth(token, exchange, chain);
            });
    }

    private Mono<Void> validateTokenWithInternalAuth(String token,ServerWebExchange exchange , WebFilterChain chain) {
        return jwtDecoder.decode(token)
            .then(chain.filter(exchange))
            .onErrorResume(err -> {
                log.error("Error validating token with internal-auth, text: {}",err.getMessage(), err );
                return errorResponse(exchange, HttpStatus.UNAUTHORIZED,"401", "Unauthorized", err.getMessage());
            });
    }

    private Mono<Map<String, String>> decodeJwt(String token) {
        return Mono.just(Objects.requireNonNull(JwtHelper.decode(token)));
    }

    private boolean isInternalAuthServer(Map<String, String> jwtDecoded) {
        assert jwtDecoded != null;
        var issuer = jwtDecoded.getOrDefault("iss", "");
        return appSetting.authentication.isInternalAuthServer(issuer);
    }

    public Mono<TokenIntrospectionResultModel> callToIntrospectEndpoint(String issuer, String token) {
        var headers = new HttpHeaders();
        headers.setBasicAuth(
            appSetting.authentication.getIntrospectionUser(issuer),
            appSetting.authentication.getIntrospectionPassword(issuer)
        );
        return webClient.postFormDataAsync(appSetting.authentication.getIntrospectionUrlByIssuer(issuer), token, headers)
            .flatMap(response -> {
                if (response.getStatusCode() != HttpStatus.OK) {
                    return Mono.just(TokenIntrospectionResultModel.failed());
                }
                var resultJson = SerializationWrapper.deserialize(response.getBody(), JsonObject.class);
                var active = JsonObjectHelper.getAsStringOrEmpty(resultJson,"active");
                return active.equalsIgnoreCase("true")
                    ? Mono.just(TokenIntrospectionResultModel.success(resultJson))
                    : Mono.just(TokenIntrospectionResultModel.failed());
            });
    }

    private Mono<Void> errorResponse(ServerWebExchange exchange, HttpStatus httpStatus, String errorCode, String errorMessage, String errorDescription) {
        exchange.getResponse().setStatusCode(httpStatus);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        var responseBody = Response.failure(errorCode, errorMessage, errorDescription);
        byte[] bytes = responseBody.getBytes();
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                .bufferFactory()
                .wrap(bytes)))
            .then(Mono.empty());
    }
}