package kh.com.cellcard.common.configuration.custom;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import kh.com.cellcard.common.configuration.appsetting.ApplicationConfiguration;
import kh.com.cellcard.common.helper.RsaHelper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Configuration
public class CustomJwtConfig {

    private final ApplicationConfiguration appSetting;

    public CustomJwtConfig(ApplicationConfiguration appSetting) {
        this.appSetting = appSetting;
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        return NimbusReactiveJwtDecoder.withJwkSetUri(appSetting.authentication.jwksUrl).build();
    }

    @Bean
    public NimbusJwtEncoder jwtEncoder() {

        KeyPair keyPair = RsaHelper.generateRsaKeyPair();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();

        // Create an RSA JWK with the private key
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID("my-private-key-id")
            .algorithm(JWSAlgorithm.RS256)
            .build();

        // Create an ImmutableJWKSet
        JWKSet jwkSet = new JWKSet(rsaKey);
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(jwkSet);

        // Create the NimbusJwtEncoder
        return new NimbusJwtEncoder(jwkSource);
    }
}
