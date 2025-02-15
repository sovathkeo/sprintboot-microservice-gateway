package kh.com.cellcard;

import kh.com.cellcard.common.wrapper.DataBaseWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.r2dbc.core.DatabaseClient;

@SpringBootApplication
public class GatewayBoilerplateApplication {

	@Autowired
	ApplicationContext context;

	@Value("${spring.application.name}")
	String name;

	public static void main(String[] args) {
		SpringApplication.run(GatewayBoilerplateApplication.class, args);
	}

	@Bean
	public DatabaseClient databaseClient() {
		var env = context.getEnvironment();
		return DataBaseWrapper.createR2DbcDatabaseClient();
	}


}
