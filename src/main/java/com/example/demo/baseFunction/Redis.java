package com.example.demo.baseFunction;

import com.example.demo.CommonFunction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;

import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

@Log4j2
@RestController
public class Redis {

    public static class DatabaseType {
        //目前测试阿里云0-20可用，其中4，8不可用
        final static public int WECHAT_LOGIN = 0;
        final static public int WECHAT_SQL = 1;
        final static public int LOGIN = 2;
        final static public int SQL = 3;
        final static public int BASE_SEAT_MAP = 5;
        final static public int ACTIVITY_SEAT_MAP = 6;
        final static public int MONGO_DB = 7;
        final static public int BILL_LOCK = 9;
        final static public int VERIFIED_CODE = 10;
        final static public int DEFAULT = 11;
        final static public int COUNT_MAP = 12;
        final static public int SHIT_MZ_CACHE = 13;
        final static public int COMMON_PARAM = 14;
    }

    static Gson gson = new Gson();
    static ObjectMapper json = new ObjectMapper();
    static boolean testMode = Environment.getTestMode();
    static boolean innerMode = Environment.getInnerMode();
    boolean shortConnect = true;
    AtomicLong lastCloseTime = new AtomicLong(CommonFunction.getTimestamp());

    Jedis redis;
    static String innerPath = Config.getConfig(testMode ? "spring.redis.host.test.inner" : "spring.redis.host.formal.inner"),
            outerPath = Config.getConfig("spring.redis.host.outer"),
            userName = Config.getConfig("spring.redis.client-name"),
            password = Config.getConfig("spring.redis.password");
    static int innerPort = Integer.parseInt(Objects.requireNonNull(Config.getConfig("spring.redis.port.inner"))),
            outerPort = Integer.parseInt(Objects.requireNonNull(Config.getConfig(testMode ? "spring.redis.port.outer.test" : "spring.redis.port.outer.formal")));
    int dbIndex = 0;
    boolean randomTime;

    public Redis() {
        init(0, true, true);
    }

    public Redis(boolean shortConnect) {
        init(0, true, shortConnect);
    }

    public Redis(int dbIndex, boolean shortConnect) {
        init(dbIndex, true, shortConnect);
    }

    public Redis(int dbIndex, boolean randomTime, boolean shortConnect) {
        init(dbIndex, randomTime, shortConnect);
    }

    void init(int dbIndex, boolean randomTime, boolean shortConnect) {
        this.dbIndex = dbIndex;
        this.randomTime = randomTime;
        this.shortConnect = shortConnect;
    }

    int randomTime(int time) {
        if (!randomTime)
            return time;
        Random random = new Random();
        return (int) (time * (random.nextDouble() + 0.5));
    }

    long randomTime(long time) {
        if (!randomTime)
            return time;
        Random random = new Random();
        return (long) (time * (random.nextDouble() + 0.5));
    }

    void connect() {
        if (innerMode)
            try {
                redis = new Jedis(innerPath, innerPort);
            } catch (Exception e) {
                e.printStackTrace();
                redis = new Jedis(outerPath, outerPort);
            }
        else
            redis = new Jedis(outerPath, outerPort);
        if (password != null && !Objects.equals(password, ""))
            redis.auth(userName, password);
        redis.select(dbIndex);
    }

    public void reconnect() {
        close();
        connect();
    }

    public void close() {
        try {
            if (redis != null)
                redis.close();
//        long timestamp = CommonFunction.getTimestamp();
//        if (redis != null && timestamp - lastCloseTime.get() > 10L) {
//            lastCloseTime.set(timestamp);
//        }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean setHard(String key, Object value) {
        reconnect();
        redis.set(key, gson.toJson(value));
        try {
            redis.save();
        } catch (Exception ignored) {
        }
        boolean result = redis.exists(key);
        if (shortConnect)
            close();
        return result;
    }

    public boolean setHard(String key, Object value, long time) {
        reconnect();
        time = randomTime(time);
        redis.setex(key, time, gson.toJson(value));
        try {
            redis.save();
        } catch (Exception ignored) {
        }
        boolean result = redis.exists(key);
        if (shortConnect)
            close();
        return result;
    }

    public boolean set(String key, String value) {
        reconnect();
        if (!redis.exists(key))
            redis.set(key, value);
        try {
            redis.save();
        } catch (Exception ignored) {
        }
        boolean result = redis.exists(key);
        if (shortConnect)
            close();
        return result;
    }

    public boolean set(String key, String value, long time) {
        reconnect();
        time = randomTime(time);
        redis.setex(key, time, value);
        try {
            redis.save();
        } catch (Exception ignored) {
        }
        boolean result = redis.exists(key);
        if (shortConnect)
            close();
        return result;
    }

    public boolean containKey(String key) {
        reconnect();
        log.info(key);
        boolean result = redis.exists(key);
        if (shortConnect)
            close();
        return result;
    }

    public String setBg(String key, String value) {
        reconnect();
        String result = redis.set(key, value);
//        return redis.bgsave();
        if (shortConnect)
            close();
        return result;
    }

    public String setBg(String key, String value, long time) {
        reconnect();
        time = randomTime(time);
        String result = redis.setex(key, time, value);
//        return redis.bgsave();
        if (shortConnect)
            close();
        return result;
    }

    public boolean setAsync(String key, Object value, int time) {
        boolean result = set(key, gson.toJson(value), time);
//        Thread t = new Thread(() ->
//        );
//        t.start();
        if (shortConnect)
            close();
        return result;
    }

    public String get(String key) {
        reconnect();
        if (!redis.exists(key))
            return null;
        String result = redis.get(key);
        if (shortConnect)
            close();
        return result;
    }

    public long remove(String key) {
        reconnect();
        long result = redis.unlink(key);
        if (shortConnect)
            close();
        return result;
    }

    public long delete(String key) {
        reconnect();
        long result = redis.del(key);
        if (shortConnect)
            close();
        return result;
    }

    public Long setTime(String key, long time) {
        reconnect();
        time = randomTime(time);
        Long result = redis.expire(key, time);
        if (shortConnect)
            close();
        return result;
    }

    public Long setTimeAt(String key, long time) {
        reconnect();
        time = randomTime(time);
        Long result = redis.expireAt(key, time);
        if (shortConnect)
            close();
        return result;
    }

    public Long getTime(String key) {
        reconnect();
        Long result = redis.ttl(key);
        if (shortConnect)
            close();
        return result;
    }

    public Set<String> getAllKeys() {
        reconnect();
        Set<String> result = redis.keys("*");
        if (shortConnect)
            close();
        return result;
    }

    public String cleanAllData() {
        reconnect();
        String result = redis.flushDB();
        if (shortConnect)
            close();
        return result;
    }
}