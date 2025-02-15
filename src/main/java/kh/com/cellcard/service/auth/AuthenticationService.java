package kh.com.cellcard.service.auth;

import kh.com.cellcard.common.configuration.appsetting.ApplicationConfiguration;
import kh.com.cellcard.common.enums.AuthScheme;
import kh.com.cellcard.common.helper.StringHelper;
import kh.com.cellcard.common.wrapper.Base64Wrapper;
import kh.com.cellcard.model.auth.AuthenticationModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class AuthenticationService {

    private static final String AUTH_TOKEN_HEADER_NAME = "X-API-KEY";
    private static final String AUTH_TOKEN = "token";

    @Autowired
    private ApplicationConfiguration appSetting;

    public AuthenticationModel authenticate(String authHeaderKey, String authHeader) {
        authHeaderKey = authHeaderKey.toLowerCase();
        return switch (authHeaderKey) {
            case "x-api-key" -> xApiKeyAuthenticate(authHeader);
            case "authorization" -> oAuthAuthenticate(authHeader);
            default -> AuthenticationModel.unAuthorized("auth header scheme not allowed");
        };
    }

    public AuthenticationModel oAuthAuthenticate(String authToken) {
        var authScheme = getAuthSchemeFromAuthHeader(authToken);
        if (authScheme == AuthScheme.None) {
            return AuthenticationModel.unAuthorized("auth scheme[%s] not allowed".formatted(authScheme.toString()));
        }
        return switch (authScheme) {
            case Basic -> basicAuthenticate(authToken);
            case Bearer -> bearerAuthenticate(authToken);
            default -> AuthenticationModel.unAuthorized("oauth scheme not allowed");
        };
    }

    public AuthenticationModel basicAuthenticate(String authToken) {

        var credentialToken = authToken.split(" ");

        var credential = Base64Wrapper.decode(credentialToken[1]);
        if (StringHelper.isNullOrEmpty(credential)) {
            return AuthenticationModel.unAuthorized("basic auth credential empty");
        }
        var credentials = credential.split(":");
        var username = credentials[0];
        var password = credentials[1];
        var isFoundUser =  Arrays.stream(appSetting
            .authentication
            .basicAuthUsers)
            .anyMatch(user -> user.username.equalsIgnoreCase(username) && user.password.equalsIgnoreCase(password));

        return isFoundUser
            ? AuthenticationModel.success()
            : AuthenticationModel.unAuthorized("Invalid username or password");
    }

    public AuthenticationModel bearerAuthenticate(String authToken) {
        return AuthenticationModel.unAuthorized("bearer auth not yet implement");
    }

    public AuthenticationModel xApiKeyAuthenticate(String authToken) {
        return AuthenticationModel.unAuthorized("x-api-key not yet implement");
    }

    public AuthScheme getAuthSchemeFromAuthHeader(String authHeader) {
        if (StringHelper.isNullOrEmpty(authHeader)) {
            return AuthScheme.None;
        }
        authHeader = authHeader.toLowerCase();
        if (authHeader.startsWith("basic ")) {
            return AuthScheme.Basic;
        } else if (authHeader.startsWith("bearer ")) {
            return AuthScheme.Bearer;
        }

        return AuthScheme.None;
    }

}
