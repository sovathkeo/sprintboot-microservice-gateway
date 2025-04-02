
package kh.com.cellcard.common.wrapper;


import jakarta.annotation.PostConstruct;
import kh.com.cellcard.common.configuration.appsetting.ApplicationConfiguration;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Slf4j
@Getter
@Component
public class WebClientWrapper {

    private WebClient.Builder webClientBuilder;

    @Autowired
    private ApplicationConfiguration appSetting;

    @Setter
    private int REQUEST_TIME_OUT = 3600;

    @PostConstruct
    public void init() {
        var httpClient = HttpClient.create()
            .responseTimeout(Duration.ofMillis(appSetting.getGlobalRequestTimeoutMillisecond()));
        webClientBuilder =  WebClient
            .builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            /*.filter((req, next) ->
                ExchangeInterceptorFunctions.addCorrelationIdHeader(req, super.getCorrelationId(), next))*/;
    }

    public WebClientWrapper useBasicAuthentication(String username, String password) {
        webClientBuilder
            .filter(ExchangeFilterFunctions.basicAuthentication(username, password));
        return this;
    }


    public ResponseEntity<?> getSync(String url) {

        return  webClientBuilder
            .build()
            .get()
            .uri(url)
                .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .toEntity(Object.class)
            .timeout(Duration.ofMillis(this.getRequestTimeout()))
            .onErrorResume(err -> {
                log.error("url[{}]; text[{}]", url, err.getMessage(), err);
                return Mono.error(err);
            })
            .block();
    }

    public Mono<ResponseEntity<String>> getAsync( String url) {
        return  webClientBuilder
            .build()
            .get()
            .uri(url)
            .retrieve()
            .toEntity(String.class)
            .timeout(Duration.ofMillis(this.getRequestTimeout()))
            .onErrorResume(err -> {
                log.error("url[{}]; text[{}]",url, err.getMessage(), err);
                return Mono.error(err);
            })
        ;
    }

    public Mono<ResponseEntity<String>> postJsonAsync(String url, Object payload) {
        return  webClientBuilder
            .build()
            .post()
            .uri(url)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(payload)
            .retrieve()
            .toEntity(String.class)
            .timeout(Duration.ofMillis(this.getRequestTimeout()))
            .onErrorResume(err -> {
                log.error("url[{}]; text[{}]", url, err.getMessage(), err);
                return Mono.error(err);
            });
    }

    public Mono<ResponseEntity<String>> postFormDataAsync(String url, String body, HttpHeaders httpHeaders) {
        return  webClientBuilder
            .build()
            .post()
            .uri(url)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .bodyValue("token=%s".formatted(body))
            .headers(headers -> headers.addAll(httpHeaders))
            .retrieve()
            .toEntity(String.class)
            .timeout(Duration.ofMillis(this.getRequestTimeout()))
            .onErrorResume(err -> {
                log.error("url[{}]; text[{}]", url, err.getMessage(), err);
                return Mono.error(err);
            });
    }

    private int getRequestTimeout() {
        return REQUEST_TIME_OUT < 1
            ? appSetting.globalRequestTimeoutMillisecond
            : REQUEST_TIME_OUT;
    }

}
