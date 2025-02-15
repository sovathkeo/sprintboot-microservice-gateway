package kh.com.cellcard.common.middleware.webfilter;

import jakarta.annotation.Nonnull;
import kh.com.cellcard.common.auth.CustomAuthentication;
import kh.com.cellcard.common.configuration.appsetting.ApplicationConfiguration;
import kh.com.cellcard.common.constant.HttpHeaderConstant;
import kh.com.cellcard.common.helper.HttpRequestHelper;
import kh.com.cellcard.common.helper.StringHelper;
import kh.com.cellcard.model.auth.AuthenticationModel;
import kh.com.cellcard.model.response.Response;
import kh.com.cellcard.service.auth.AuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(1)
public class CustomAuthenticationWebFilter extends BaseWebFilter implements WebFilter {

    private final Logger logger = LoggerFactory.getLogger(CustomAuthenticationWebFilter.class);

    @Autowired
    private ApplicationConfiguration appSetting;

    @Autowired
    private AuthenticationService authService;

    @Autowired
    private CustomAuthentication customAuth;

    @Nonnull
    @Override
    public Mono<Void> filter(@Nonnull ServerWebExchange exchange, @Nonnull WebFilterChain chain) {

        var request = exchange.getRequest();
        var correlationId = getCorrelationIdFromRequest(request);
        // Add the correlation ID to MDC (Mapped Diagnostic Context) for logging
        MDC.put(HttpHeaderConstant.CORRELATION_ID, correlationId);

        // If there is no config mark as by-pass auth
        if ( appSetting == null || !isAuthEnabled()) {
            var authTypeConfig = appSetting == null ? "auth config null" : appSetting.authentication.type;
            logger.info("by-pass auth; auth-type-config: %s".formatted(authTypeConfig));
            return chain.filter(exchange);
        }

        if ( appSetting.authentication.isCustomAuth() ) {
            var auth = customAuth.handleAuth(exchange);
            if (auth.isAuthenticated) {
                return chain.filter(exchange);
            }
            return returnAuthFailed(request, exchange.getResponse(), auth, correlationId);
        }

        var authHeaderKey = HttpHeaderConstant.AUTHORIZATION;
        var authHeaderValue = HttpRequestHelper.getHeaderOrDefault(exchange, HttpHeaders.AUTHORIZATION, "");

        if (StringHelper.isNullOrEmpty(authHeaderValue)) {
            authHeaderValue = HttpRequestHelper.getHeaderOrDefault(exchange, HttpHeaderConstant.X_API_KEY, "");
            if (!StringHelper.isNullOrEmpty(authHeaderKey)) {
                authHeaderKey = HttpHeaderConstant.X_API_KEY;
            }
        }

        if (StringHelper.isNullOrEmpty(authHeaderValue)) {
            return returnAuthFailed(request, exchange.getResponse(), AuthenticationModel.unAuthorized("auth header is missing"),correlationId);
        }

        var auth = authService.authenticate(authHeaderKey, authHeaderValue);

        if (auth.isAuthenticated) {
            return chain.filter(exchange);
        }

        return returnAuthFailed(request, exchange.getResponse(), auth, correlationId);
    }
    private boolean isAuthEnabled() {
        return  appSetting.authentication.enabled;
    }
    private Mono<Void> returnAuthFailed(ServerHttpRequest request, ServerHttpResponse response, AuthenticationModel authModel, String correlationId) {
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        var json = Response.unAuthorized(authModel.errorMessage, "auth failed", correlationId);
        logger.error("request unauthorized; auth-error[%s]; url[%s]".formatted(authModel.errorMessage, request.getURI().toString()));
        return response.writeWith(Mono.just(response
            .bufferFactory()
            .wrap(json.getBytes()))
        );
    }
}
