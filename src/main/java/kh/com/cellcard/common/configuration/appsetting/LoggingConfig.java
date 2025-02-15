package kh.com.cellcard.common.configuration.appsetting;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LoggingConfig {

    public Request request = new Request();

    @Getter
    @Setter
    public static class Request {
        public boolean logBody = true;
    }
}
