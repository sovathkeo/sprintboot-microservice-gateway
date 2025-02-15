package kh.com.cellcard.common.auth;

import kh.com.cellcard.model.auth.AuthenticationModel;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

@Component
public class CustomAuth extends CustomAuthenticationImpl{

    @Override
    public AuthenticationModel handleAuth(ServerWebExchange exchange) {
        return AuthenticationModel.unAuthorized("Please implement your auth at common.auth.CustomAuth!");
    }
}
