package kh.com.cellcard.common.configuration.custom;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import kh.com.cellcard.common.configuration.appsetting.ApplicationConfiguration;
import kh.com.cellcard.common.custom.CustomTokenValidator;
import kh.com.cellcard.common.helper.RsaHelper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
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
        var jwtDecoder =  NimbusReactiveJwtDecoder.withJwkSetUri(appSetting.authentication.jwksUrl).build();
        // Open code below to enable custom token validator
        /*DelegatingOAuth2TokenValidator<Jwt> jwtValidator = new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(appSetting.authentication.issuer),
            new CustomTokenValidator(appSetting)
        );
        jwtDecoder.setJwtValidator(jwtValidator);*/
        return jwtDecoder;
    }

    @Bean
    public NimbusJwtEncoder jwtEncoder() throws URISyntaxException, MalformedURLException {
        // Generate an RSA key pair (replace with your secure key retrieval)
        //KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        //keyPairGenerator.initialize(2048);

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
