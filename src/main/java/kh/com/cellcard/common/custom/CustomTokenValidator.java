package kh.com.cellcard.common.custom;

import kh.com.cellcard.common.configuration.appsetting.ApplicationConfiguration;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoders;
import org.springframework.stereotype.Component;

//@Component
public class CustomTokenValidator implements OAuth2TokenValidator<Jwt> {

    private ApplicationConfiguration appSetting;
    private ReactiveJwtDecoder jwtDecoder;

    public CustomTokenValidator(ApplicationConfiguration appSetting) {
        this.appSetting = appSetting;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        jwtDecoder = ReactiveJwtDecoders.fromIssuerLocation(appSetting.authentication.issuer);
        var s = jwtDecoder.decode(token.getTokenValue())
            .doOnSuccess(jwt -> {
                System.out.println(jwt);
            })
            .subscribe();
        return OAuth2TokenValidatorResult.success();
    }
}
