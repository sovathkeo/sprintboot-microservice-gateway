package kh.com.cellcard.model.auth;

import kh.com.cellcard.common.enums.AuthScheme;

public class AuthenticationModel {
    public String authToken;
    public AuthScheme authScheme = AuthScheme.None;
    public boolean isAuthenticated;
    public String errorMessage;

    private AuthenticationModel(String errorMessage, boolean isAuthenticated) {
        this.isAuthenticated = isAuthenticated;
        this.errorMessage = errorMessage;
    }

    private AuthenticationModel(AuthScheme authScheme,String errorMessage, boolean isAuthenticated) {
        this.isAuthenticated = isAuthenticated;
        this.errorMessage = errorMessage;
        this.authScheme = authScheme;
    }

    public static AuthenticationModel unAuthorized(String error) {
        return new AuthenticationModel(error, false);
    }

    public static AuthenticationModel success() {
        return new AuthenticationModel(AuthScheme.None, "success", true);
    }
}
