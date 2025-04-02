package kh.com.cellcard.common.configuration.appsetting;

import lombok.Data;

@Data
public class AuthServerConfig {
    public String name;
    public String[] issuers;
    public String introspectUrl;
    public String username;
    public String password;
}
