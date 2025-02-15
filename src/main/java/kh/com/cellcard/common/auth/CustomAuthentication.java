package kh.com.cellcard.common.auth;

import kh.com.cellcard.model.auth.AuthenticationModel;
import org.springframework.web.server.ServerWebExchange;

public interface CustomAuthentication {
    default AuthenticationModel handleAuth(ServerWebExchange exchange) {
        return AuthenticationModel.unAuthorized("custom authentication not implemented!");
    }
}
