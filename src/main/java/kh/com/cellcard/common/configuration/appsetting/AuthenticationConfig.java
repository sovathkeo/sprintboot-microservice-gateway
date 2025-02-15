package kh.com.cellcard.common.configuration.appsetting;

import kh.com.cellcard.common.constant.AuthenticationConstant;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class AuthenticationConfig {
    public boolean enabled = false;
    public String type = AuthenticationConstant.NoAuth;
    public BasicAuthUserConfig[] basicAuthUsers = new BasicAuthUserConfig[0];

    @Setter
    @Getter
    public static class BasicAuthUserConfig {
        public String username = "";
        public String password = "";
    }

    public boolean isCustomAuth() {
        return AuthenticationConstant.Custom.equalsIgnoreCase(type);
    }
}
