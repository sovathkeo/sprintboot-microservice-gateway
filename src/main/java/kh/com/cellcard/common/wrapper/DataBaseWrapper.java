package kh.com.cellcard.common.wrapper;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.springframework.r2dbc.core.DatabaseClient;

import java.time.Duration;

import static io.r2dbc.spi.ConnectionFactoryOptions.*;

public abstract class DataBaseWrapper {

    public static ConnectionFactory createR2DbcConnection() {
        ConnectionFactoryOptions options = ConnectionFactoryOptions.builder()
            .option(HOST, "172.16.101.29")
            .option(PORT, 1433)
            .option(DRIVER, "sqlserver")
            .option(PROTOCOL, "r2dbc")
            .option(DATABASE, "STG")
            .option(USER, "devadmin")
            .option(PASSWORD, "V@!aDmin2020Devusr")
            .build();

        ConnectionPoolConfiguration poolConfiguration = ConnectionPoolConfiguration.builder(ConnectionFactories.get(options))
            .maxIdleTime(Duration.ofMinutes(30))
            .initialSize(10)
            .maxSize(20)
            .build();

        return new ConnectionPool(poolConfiguration);
    }

    public static DatabaseClient createR2DbcDatabaseClient() {
        return DatabaseClient.create(createR2DbcConnection());
    }

}
