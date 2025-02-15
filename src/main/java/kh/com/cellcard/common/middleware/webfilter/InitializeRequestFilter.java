package kh.com.cellcard.common.middleware.webfilter;

import jakarta.annotation.Nonnull;
import kh.com.cellcard.common.configuration.appsetting.ApplicationConfiguration;
import kh.com.cellcard.common.constant.HttpHeaderConstant;
import kh.com.cellcard.common.helper.HttpRequestHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(2)
public class InitializeRequestFilter extends BaseWebFilter implements WebFilter {

    private final Logger logger = LoggerFactory.getLogger(InitializeRequestFilter.class);

    @Autowired
    private ApplicationConfiguration appSetting;

    @NonNull
    @Override
    public Mono<Void> filter(@Nonnull ServerWebExchange exchange, @Nonnull WebFilterChain chain) {

        var request = exchange.getRequest();
        var correlationId = getCorrelationIdFromRequest(request);
        // Add the correlation ID to MDC (Mapped Diagnostic Context) for logging
        MDC.put(HttpHeaderConstant.CORRELATION_ID, correlationId);

        // Add the correlation ID to the response headers
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().add(HttpHeaderConstant.CORRELATION_ID, correlationId);

        // Add the correlation ID to the request attribute for downstream handlers
        ServerHttpRequest modifiedRequest = request.mutate()
            .header(HttpHeaderConstant.CORRELATION_ID, correlationId)
            .build();
        ServerWebExchange modifiedExchange = exchange.mutate().request(modifiedRequest).build();
        var isLogBody = appSetting.logging.request.logBody;
        if (HttpRequestHelper.hasBody(request) && isLogBody) {
            return this.logRequestBody(request, chain, modifiedExchange);
        }
        logger.info("received request");
        return chain
            .filter(modifiedExchange)
            .doFinally(signalType -> clearCorrelationAndLogResponse());

    }
}
