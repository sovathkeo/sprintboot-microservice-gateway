package kh.com.cellcard.common.configuration.appsetting;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import kh.com.cellcard.common.helper.StringHelper;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.wavefront.WavefrontProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "application-config")
@JsonIgnoreProperties(value = {"$$beanFactory"})
@Setter
@Getter
public class ApplicationConfiguration {
    public static final String APPLICATION_NAME = WavefrontProperties.Application.class.getPackageName();

    @Getter
    public GlobalConfig globalConfig = new GlobalConfig();

    public int globalRequestTimeoutMillisecond;
    public int globalConnectTimeoutMillisecond;

    @Value("${spring.application.name}")
    private String applicationName;

    @Getter
    public String maintenanceMode = "";

    public String maintenanceMessage = "";

    private String[] endpointsAuthWhitelist = new String[]{};

    public LoggingConfig logging = new LoggingConfig();

    public AuthenticationConfig authentication = new AuthenticationConfig();

    public String getApplicationName() {
        return StringHelper.isNullOrEmpty(applicationName) ? APPLICATION_NAME : applicationName;
    }

    public String getMaintenanceMessage() {
        return StringHelper.isNullOrEmpty(maintenanceMessage)
                ? this.globalConfig.maintenanceMessage
                : this.maintenanceMessage;
    }

}