package kh.com.cellcard.common.configuration.custom;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

@Configuration
public class CustomJwtConfig {

    private final String JWKS_URL = "http://localhost:9000/oauth2/jwks";

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        return NimbusReactiveJwtDecoder.withJwkSetUri(JWKS_URL).build();
    }

}
