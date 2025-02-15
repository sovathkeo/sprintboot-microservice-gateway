package kh.com.cellcard.common.configuration.custom;

import kh.com.cellcard.common.custom.CustomDynamicRedisRateLimiter;
import kh.com.cellcard.common.custom.IpKeyResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.core.DatabaseClient;

import java.util.Map;

@Configuration
public class CustomGatewayConfig {

    @Autowired
    private DatabaseClient databaseClient;

    @Autowired
    private IpKeyResolver ipKeyResolver;

    //@Autowired
    //private RedisRateLimiter redisRateLimiter;

    @Autowired
    private CustomDynamicRedisRateLimiter customDynamicRedisRateLimiter;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {

        var dbRoutes = databaseClient.sql("select route_id, route_path, route_is_rewrite, route_rewrite_regex, route_rewrite_replacement, route_uri from test.ms_gateway_route")
            .fetch()
            .all()
            .collectList()
            .block()
            ;

        var routes = builder.routes();

        for (Map<String, Object> dbRoute : dbRoutes) {
            var routeId = dbRoute.get("route_id").toString();
            routes.route(
                    routeId,
                    r -> r
                        .path(dbRoute.get("route_path").toString())
                        .filters(f -> {
                            if ((boolean)dbRoute.get("route_is_rewrite")) {
                                var regex = dbRoute.get("route_rewrite_regex").toString();
                                var replacement = dbRoute.get("route_rewrite_replacement").toString();
                                f.rewritePath(regex, replacement);
                            }

                            f.requestRateLimiter(cfg ->
                                cfg.setRateLimiter(customDynamicRedisRateLimiter)
                                    .setKeyResolver(ipKeyResolver)
                            );
                            return f;
                        })
                        .uri(dbRoute.get("route_uri").toString())
                );
        }

        return routes.build();
    }
}
