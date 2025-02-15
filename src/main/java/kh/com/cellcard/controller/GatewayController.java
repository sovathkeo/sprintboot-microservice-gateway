package kh.com.cellcard.controller;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.internal.LettuceClassUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.HashMap;

@RestController
//@RequestMapping("/gate-way/v1")
public class GatewayController {

    @Autowired
    RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RedisClient redisClient;

    @GetMapping(value = "/name", produces = MediaType.ALL_VALUE)
    public Object getName(@RequestParam("name") String name, @RequestHeader HttpHeaders header) {

       // var s = redisTemplate.opsForValue().get("admin");

        var ss = redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE))
            .sync()
            .get("admin");

        var proxyManager = LettuceBasedProxyManager.builderFor(redisClient)
            .build();

        BucketConfiguration bucketConfiguration = BucketConfiguration.builder()
            .addLimit(Bandwidth.builder()
                .capacity(1)
                .refillIntervally(1, Duration.ofSeconds(1))
                .build()
            )
            .build();
        RemoteBucketBuilder<byte[]> builder = proxyManager.builder();

        var b = builder.build("admin".getBytes(), () -> bucketConfiguration);

        return new HashMap<>() {
            {
                put("name", name);
            }
        };
    }
}
