package com.example.demo.baseFunction;

import com.example.demo.CommonFunction;
import com.sun.management.OperatingSystemMXBean;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
//@RestController
//内部redis，仿redis
public class InnerCache {
    @Builder
    static class CacheCore {
        public long expiredTime;
        public String value;
    }

    public static Map<Integer, Map<String, CacheCore>> mainCache = new HashMap<>();

    InnerCache() {
        setValue(0, "test", "122", 12222);
        String t = getValue(0, "test");
        log.info(t);
    }

    private static final OperatingSystemMXBean operatingSystemMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

    static CacheCore getCore(int dbIndex, String key) {
        if (!mainCache.containsKey(dbIndex) || mainCache.get(dbIndex) == null)
            return null;
        CacheCore core = mainCache.get(dbIndex).get(key);
        if (core.expiredTime >= 0 && core.expiredTime < CommonFunction.getTimestamp()) {
            //过期
            mainCache.get(dbIndex).remove(key);
            return null;
        }
        return core;
    }

    public static String getValue(int dbIndex, String key) {
        CacheCore core = getCore(dbIndex, key);
        if (core == null)
            return null;
        return core.value;
    }

    public static int getFreeMemoryRatio() {
        double totalMemorySize = operatingSystemMXBean.getTotalMemorySize();
        double freeMemorySize = operatingSystemMXBean.getFreeMemorySize();
        double value = freeMemorySize / totalMemorySize;
        return (int) ((1 - value) * 100);
    }

    static void autoFreeMemory() {
        Thread thread = new Thread(() -> {
//            long size = JSON.toJSONString(mainCache).length();
            if (getFreeMemoryRatio() > 90)
//            if (size > 1024 * 1024 * 1024)//如果缓存大于2G，清除固定与过期内容
                cleanAllExpiredData();
        });
        thread.start();
    }

    static void cleanAllExpiredData() {
        long timestamp = CommonFunction.getTimestamp();
        mainCache.replaceAll((k, v) -> v.entrySet().parallelStream().filter(s ->
                s.getValue().expiredTime > 0 && s.getValue().expiredTime < timestamp
        ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    static void setValueCore(int dbIndex, String key, String value, long time_ms) {
        if (!mainCache.containsKey(dbIndex) || mainCache.get(dbIndex) == null)
            mainCache.put(dbIndex, new HashMap<>());
        CacheCore core = CacheCore.builder().value(value).expiredTime(time_ms < 0 ? -1 : CommonFunction.getTimestamp() + time_ms).build();
        mainCache.get(dbIndex).put(key, core);
        autoFreeMemory();
    }

    public static void setValue(int dbIndex, String key, String value, long time_s) {
        setValueCore(dbIndex, key, value, time_s < 0 ? -1 : time_s * 1000);
    }

    public static void setValue(int dbIndex, String key, String value) {
        setValue(dbIndex, key, value, -1);
    }

    public static boolean setTime(int dbIndex, String key, long time_s) {
        if (!mainCache.containsKey(dbIndex) || mainCache.get(dbIndex) == null)
            return false;
        CacheCore core = getCore(dbIndex, key);
        if (core == null)
            return false;
        core.expiredTime = time_s < 0 ? -1 : CommonFunction.getTimestamp() + time_s * 1000;
        mainCache.get(dbIndex).put(key, core);
        return true;
    }
}
