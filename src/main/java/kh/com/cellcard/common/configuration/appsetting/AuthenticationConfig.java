package kh.com.cellcard.common.configuration.appsetting;

import kh.com.cellcard.common.constant.AuthenticationConstant;
import kh.com.cellcard.common.helper.StringHelper;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.stream.Stream;

@Data
public class AuthenticationConfig {
    public boolean enabled = false;
    public String type = AuthenticationConstant.NoAuth;
    public BasicAuthUserConfig[] basicAuthUsers = new BasicAuthUserConfig[0];
    public String jwksUrl;
    public String issuer;
    public AuthServerConfig[] authServers = new AuthServerConfig[0];

    @Setter
    @Getter
    public static class BasicAuthUserConfig {
        public String username = "";
        public String password = "";
    }

    public boolean isInternalAuthServer(String issuer) {
        if (authServers.length < 1) {
            return false;
        }
        return Arrays.stream(authServers)
            .anyMatch(auth ->
                auth.name.equalsIgnoreCase("internal-auth") &&
                Arrays.stream(auth.issuers).anyMatch(issuer::equalsIgnoreCase)
            );
    }

    public String getIntrospectionUser(String issuer) {
        var authConfig = this.getAuthServerConfigByIssuer(issuer);
        return authConfig != null ? authConfig.username : "";
    }

    public String getIntrospectionPassword(String issuer) {
        var authConfig = this.getAuthServerConfigByIssuer(issuer);
        return authConfig != null ? authConfig.password : "";
    }

    public String getIntrospectionUrlByIssuer(String issuer) {
        var authConfig = this.getAuthServerConfigByIssuer(issuer);
        return authConfig != null ? authConfig.introspectUrl : "";
    }

    public AuthServerConfig getAuthServerConfigByIssuer(String issuer) {
        if (isAuthServerConfigEmpty()) {
            return null;
        }
        return Stream.of(authServers)
            .filter(auth -> Stream.of(auth.issuers).anyMatch(issuer::equalsIgnoreCase))
            .findFirst()
            .orElse(null);
    }

    public boolean isCustomAuth() {
        return AuthenticationConstant.Custom.equalsIgnoreCase(type);
    }

    public boolean isAuthServerConfigEmpty() {
        return this.authServers == null || authServers.length < 1;
    }
}
