package kh.com.cellcard.common.middleware.webfilter;

import jakarta.annotation.Nonnull;
import kh.com.cellcard.common.constant.HttpHeaderConstant;
import kh.com.cellcard.common.wrapper.UuidWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

public abstract class BaseWebFilter {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected Mono<Void> logRequestBody(ServerHttpRequest request, WebFilterChain chain, ServerWebExchange modifiedExchange) {
        return DataBufferUtils.join(request.getBody())
            .flatMap(dataBuffer -> {
                String requestBody = dataBuffer.toString(StandardCharsets.UTF_8);

                logger.info("received request; body : {}", requestBody);
                // Create a new request with the buffered body

                ServerHttpRequest mutatedRequest = request.mutate().build();
                DataBufferUtils.retain(dataBuffer);
                Flux<DataBuffer> cachedBodyFlux = Flux.defer(
                    () -> Flux.just(dataBuffer.slice(0, dataBuffer.readableByteCount())));
                mutatedRequest = new ServerHttpRequestDecorator(mutatedRequest) {

                    @Nonnull
                    @Override
                    public Flux<DataBuffer> getBody() {
                        return cachedBodyFlux;
                    }
                };

                return chain
                    .filter(modifiedExchange.mutate().request(mutatedRequest).build())
                    .doFinally(signalType -> clearCorrelationAndLogResponse());
            });
    }

    protected String getCorrelationIdFromRequest(ServerHttpRequest request) {
        if (!request.getHeaders().containsKey(HttpHeaderConstant.CORRELATION_ID)) {
            return UuidWrapper.uuidAsString();
        }
        return request.getHeaders().getFirst(HttpHeaderConstant.CORRELATION_ID) != null ?
            request.getHeaders().getFirst(HttpHeaderConstant.CORRELATION_ID) :
            UuidWrapper.uuidAsString();
    }

    protected void clearCorrelationAndLogResponse() {
        logger.info("responded");
        MDC.remove(HttpHeaderConstant.CORRELATION_ID);
    }
}
