package com.illtamer.service.currencytrading.common.redis;

import com.illtamer.service.currencytrading.common.util.ClassPathUtil;
import io.lettuce.core.Range;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.support.ConnectionPoolSupport;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.function.Consumer;

@Service
public class RedisService {

    private static final Logger log = LoggerFactory.getLogger(RedisService.class);

    private final RedisClient redisClient;
    private final GenericObjectPool<StatefulRedisConnection<String, String>> redisConnectionPool;

    @Autowired
    public RedisService(RedisConfiguration redisConfig) {
        RedisURI uri = RedisURI.Builder.redis(redisConfig.getHost(), redisConfig.getPort())
                .withPassword(redisConfig.getPassword().toCharArray()).withDatabase(redisConfig.getDatabase()).build();
        this.redisClient = RedisClient.create(uri);

        GenericObjectPoolConfig<StatefulRedisConnection<String, String>> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(20);
        poolConfig.setMaxIdle(5);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        this.redisConnectionPool = ConnectionPoolSupport.createGenericObjectPool(redisClient::connect, poolConfig);
    }

    @PreDestroy
    public void shutdown() {
        this.redisConnectionPool.close();
        this.redisClient.shutdown();
    }

    /**
     * Load Lua script from classpath file and return SHA as string.
     *
     * @param classpathFile Script path.
     * @return SHA as string.
     */
    public String loadScriptFromClassPath(String classpathFile) {
        String sha = executeSync(commands -> {
            try {
                return commands.scriptLoad(ClassPathUtil.readFile(classpathFile));
            } catch (IOException e) {
                throw new UncheckedIOException("load file from classpath failed: " + classpathFile, e);
            }
        });
        if (log.isInfoEnabled()) {
            log.info("loaded script {} from {}.", sha, classpathFile);
        }
        return sha;
    }

    /**
     * Load Lua script and return SHA as string.
     *
     * @param scriptContent Script content.
     * @return SHA as string.
     */
    private String loadScript(String scriptContent) {
        return executeSync(commands ->
                commands.scriptLoad(scriptContent));
    }

    public void subscribe(String channel, Consumer<String> listener) {
        StatefulRedisPubSubConnection<String, String> conn = this.redisClient.connectPubSub();
        conn.addListener(new RedisPubSubAdapter<>() {
            @Override
            public void message(String channel, String message) {
                listener.accept(message);
            }
        });
        conn.sync().subscribe(channel);
    }

    public Boolean executeScriptReturnBoolean(String sha, String[] keys, String[] values) {
        return executeSync(commands ->
                commands.evalsha(sha, ScriptOutputType.BOOLEAN, keys, values));
    }

    public String executeScriptReturnString(String sha, String[] keys, String[] values) {
        return executeSync(commands ->
                commands.evalsha(sha, ScriptOutputType.VALUE, keys, values));
    }

    public String get(String key) {
        return executeSync((commands) ->
                commands.get(key));
    }

    public void publish(String topic, String data) {
        executeSync((commands) ->
                commands.publish(topic, data));
    }

    public List<String> lrange(String key, long start, long end) {
        return executeSync((commands) ->
                commands.lrange(key, start, end));
    }

    public List<String> zrangebyscore(String key, long start, long end) {
        return executeSync((commands) ->
                commands.zrangebyscore(key, Range.create(start, end)));
    }

    public <T> T executeSync(SyncCommandCallback<T> callback) {
        try (StatefulRedisConnection<String, String> connection = redisConnectionPool.borrowObject()) {
            connection.setAutoFlushCommands(true);
            RedisCommands<String, String> commands = connection.sync();
            return callback.doInConnection(commands);
        } catch (Exception e) {
            log.warn("executeSync redis failed.", e);
            throw new RuntimeException(e);
        }
    }

}
