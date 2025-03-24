package kh.com.cellcard.model.auth;

import org.springframework.security.oauth2.jwt.Jwt;

public class AuthorizeResultModel {
    public boolean isAuthorized;
    public Jwt jwt;
    public String errorMessage;

    private AuthorizeResultModel(String errorMessage, boolean isAuthorized) {
        this.errorMessage = errorMessage;
        this.isAuthorized = isAuthorized;
    }

    private AuthorizeResultModel(Jwt jwt, boolean isAuthorized) {
        this.jwt = jwt;
        this.isAuthorized = isAuthorized;
    }

    public static AuthorizeResultModel unAuthorized(String errorMessage) {
        return new AuthorizeResultModel(errorMessage, false);
    }

    public static AuthorizeResultModel success(Jwt jwt) {
        return new AuthorizeResultModel(jwt, true);
    }
}
