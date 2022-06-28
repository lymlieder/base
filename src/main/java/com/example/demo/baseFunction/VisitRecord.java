package com.example.demo.baseFunction;

import com.example.demo.CommonFunction;
import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bson.Document;

import java.util.Map;

@Log4j2
public class VisitRecord {
    static Gson gson = new Gson();

    static String recordUrl = Environment.getK8sMode() ? Config.getConfig("k8s.external.url", "http://external:9001/") : Config.getConfig("default.external.url", "http://127.0.0.1:9001/");

    @AllArgsConstructor
    static public class RequestClass {
        public String requestID;
        public Map<String, Object> param;
        public String time;
    }

    @AllArgsConstructor
    static class RequireClass {
        public String requestId;
        public ResResult.ResultCode resultCode;
        public Object data;
        public String comment;
    }

    static public String getRequestID(long timestamp, String functionName) {
        try {
            return "w_" + timestamp + "_" + CommonFunction.getRandomString(6, functionName.hashCode() + (int) (timestamp % (Integer.MAX_VALUE / 10)));
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    static public void setVisitRecord_wechatServer(String serverType, String functionName, Boolean success, Object requestData, Object responseData, long duration) {
        new Thread(() -> {
            try {
                String dataString = responseData == null ? "" : gson.toJson(responseData);
                int responseSize = dataString.length();
                if (success && dataString.length() > 200)
                    dataString = dataString.substring(0, 200) + "...";
                Document param = new Document();
                param.put("serverType", serverType);
                param.put("functionName", functionName);
                param.put("success", success);
                param.put("requestData", gson.toJson(requestData));
                param.put("requiredData", dataString);
                param.put("responseSize", responseSize);
                param.put("from", "wechat");
                param.put("duration", duration);
                param.put("create_time", CommonFunction.getTimestamp());
                RestAPI.post(recordUrl + "visitRecord/wechatServerVisit", param, 1000);
                log.info("访问 wechat_server " + (Boolean.TRUE.equals(success) ? "成功：" : "失败：") + gson.toJson(param));
            } catch (Exception e) {
//                e.printStackTrace();
            }
        }).start();
    }

    static public void setVisitRecord_wechat(String streamId, String functionName, Boolean success, Object requestData, Object responseData, long duration) {
        new Thread(() -> {
            try {
                String dataString = responseData == null ? "" : gson.toJson(responseData);
                int responseSize = dataString.length();
                if (success && dataString.length() > 200)
                    dataString = dataString.substring(0, 200) + "...";
                Document param = new Document();
                param.put("streamId", streamId);
                param.put("functionName", functionName);
                param.put("success", success);
                param.put("requestData", gson.toJson(requestData));
                param.put("requiredData", dataString);
                param.put("responseSize", responseSize);
                param.put("from", "wechat");
                param.put("duration", duration);
                param.put("create_time", CommonFunction.getTimestamp());
                RestAPI.post(recordUrl + "visitRecord/wechatVisit", param, 1000);
                log.info("访问 wechat " + (Boolean.TRUE.equals(success) ? "成功：" : "失败：") + gson.toJson(param));
            } catch (Exception e) {
//                e.printStackTrace();
            }
        }).start();
    }
}
