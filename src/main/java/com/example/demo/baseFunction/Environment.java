package com.example.demo.baseFunction;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.log4j.Log4j2;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

@Log4j2
public class Environment {
    static Boolean testMode = null, k8sMode = null, innerMode = null;
    static Gson gson = new Gson();

    static public Map<String, Boolean> getEnvironment() {
        Map<String, Boolean> result = new HashMap<>();
        result.put("testMode", getTestMode());
        result.put("k8sMode", getK8sMode());
        result.put("innerMode", getInnerMode());
        return result;
    }

    static Map<String, Boolean> getMode() throws Exception {
        FileInputStream fileInputStream = new FileInputStream("/cert/env.config");
        String env = new String(fileInputStream.readAllBytes());
        Map<String, Boolean> result = gson.fromJson(env, new TypeToken<Map<String, Boolean>>() {
        }.getType());
        log.warn(result);
        return result;
    }

    /**
     * 是否是测试环境 测试环境连测试数据库系统，正式连正式数据库系统
     * 测试环境判断依据为，如果部署服务器存在文件/cert/env.config，json格式，并且内容项包括testMode则为testMode值，否则按照application.properties中testMode情况决定
     *
     * @return true，测试环境
     */
    static public boolean getTestMode() {
        if (testMode == null) {
            try {
                testMode = getMode().getOrDefault("testMode", Boolean.parseBoolean(Config.getConfig("test.mode", "false")));
            } catch (Exception ignored) {
                testMode = Boolean.parseBoolean(Config.getConfig("test.mode", "false"));
            } finally {
                if (testMode == null)
                    testMode = false;
            }
        }
        return testMode;
    }

    /**
     * 是否是k8s部署模式
     * k8s部署模式主要决定文件访问是相对路径还是绝对路径。需要注意的是，k8s部署只能通过绝对路径访问，需要对应k8s相应挂载路径
     * k8s模式判断依据为，部署服务器存在文件/cert/env.config，json格式，并且内容项k8sMode为true，否则均为false
     *
     * @return true，k8s部署模式
     */
    static public boolean getK8sMode() {
        if (k8sMode == null)
            try {
                k8sMode = getMode().getOrDefault("k8sMode", false);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (k8sMode == null)
                    k8sMode = false;
            }
        return k8sMode;
    }

    /**
     * 是否通过内网模式链接数据库系统
     * 内网模式判断依据为，部署服务器存在文件/cert/env.config，json格式，并且内容项innerMode为true，否则均为false
     *
     * @return true，通过内网模式连接 false，通过跳板机连接，性能受限
     */
    static public boolean getInnerMode() {
        if (innerMode == null)
            try {
                innerMode = getMode().getOrDefault("innerMode", false);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (innerMode == null)
                    innerMode = false;
            }
        return innerMode;
    }
}