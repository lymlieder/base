package com.example.demo;

import com.example.demo.baseFunction.Crypto;
import com.example.demo.baseFunction.PgSQL;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.json.JSONObject;
import org.json.XML;
import org.postgresql.util.PGobject;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

public class CommonFunction {

    static private final PgSQL sql = new PgSQL();
    static Gson gson = new Gson();
    static ObjectMapper json = new ObjectMapper();

    public enum BillType {Ticket, Product}

    static public <T> T transType(Object input, Class<T> t) {
        json.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        json.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
        return json.convertValue(input, t);
    }

    static public String getJsonBFromInput(Object input) {
        try {
            if (input == null)
                return null;
            Map<String, Object> map;
            if (String.class.equals(input.getClass()))
                map = gson.fromJson(input.toString(), new TypeToken<>() {
                }.getType());
            else if (PGobject.class.equals(input.getClass()))
                return ((PGobject) input).getValue();
            else
                map = (Map<String, Object>) input;
            if (map.containsKey("type") && Objects.equals(map.get("type").toString(), "jsonb") && map.containsKey("value")) {
                Object value = map.get("value");
                if (String.class.equals(value.getClass()))
                    return value.toString();
                return gson.toJson(value);
            }
            return gson.toJson(map);
        } catch (Exception e) {
            return gson.toJson(input);
        }
    }

    static public int getIntFromObject(Object number) {
        try {
            return ((Double) number).intValue();
        } catch (Exception e) {
            return Integer.parseInt(number.toString());
        }
    }

    static public long getLongFromObject(Object number) {
        try {
            return ((Double) number).longValue();
        } catch (Exception e) {
            return Long.parseLong(number.toString());
        }
    }

    static public String getPayBatchId(BillType billType, int client_id, long timestamp, String checkSql) throws Exception {
        int maxCount = 10;
        String payBatchId = (billType.name().charAt(0) + Crypto.MD5(String.valueOf(client_id)).substring(16) + "_" + String.valueOf(timestamp).substring(4) + "_" + CommonFunction.getRandomString(4)).substring(0, 32);
        while (sql.select(checkSql, new Object[]{payBatchId}, false).size() > 0 && maxCount > 0) {
            payBatchId = billType.name().charAt(0) + Crypto.MD5(String.valueOf(client_id)).substring(15) + "_" + String.valueOf(CommonFunction.getTimestamp()).substring(4) + "_" + CommonFunction.getRandomString(4);
            maxCount--;
        }
        if (maxCount <= 0)
            throw new Exception("payBatchId生成错误，请重试");
        return payBatchId;
    }

    static public String getLimitSkipSubSql(List<Object> paramList, Integer page_offset, Integer page_count) {
        String result = "";
        int count = 1;
        if (page_count != null) {
            paramList.add(page_count);
            count = page_count;
            result += " limit ?";
        }
        if (page_offset != null) {
            Integer skip = count * page_offset;
            paramList.add(skip);
            result += " offset ?";
        }
        return result;
    }

    /**
     * json转xml，并指定根节点
     *
     * @param json    json字符串
     * @param tagName 根节点名称
     * @return 带根节点的xml字符串
     */
    public static String json2Xml(String json, String tagName) {
        JSONObject jsonObject = new JSONObject(json);
        return XML.toString(jsonObject, tagName);
    }

    public static String json2Xml(String json) {
        JSONObject jsonObject = new JSONObject(json);
        return XML.toString(jsonObject, null);
    }

    /**
     * xml转json
     *
     * @param xml xml字符串
     * @return json字符串
     */
    public static String xml2Json(String xml) {
        return XML.toJSONObject(xml).toString();
    }

    public static Map<String, String> transToMap(String s) {
        Map<String, Object> map;
        try {
            map = gson.fromJson(s, new TypeToken<>() {
            }.getType());
        } catch (Exception e) {
            map = transToMap_core(s);
        }
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String || value instanceof Number)
                result.put(key, value.toString());
            else
                result.put(key, gson.toJson(value.toString()));
        }
        return result;
    }

    /**
     * 将网络字符串结构转换成Map
     *
     * @param s 网络返回值
     * @return 结果
     */
    public static Map<String, Object> transToMap_core(String s) {
        try {
            String[] p = s.split("&");
            Map<String, Object> map = new HashMap<>();
            for (String w : p) {
                try {
                    String[] r = w.split("=");
                    map.put(r[0], URLDecoder.decode(r[1], StandardCharsets.UTF_8));
                } catch (Exception ignored) {
                }
            }
            return map;
        } catch (Exception e) {
//            e.printStackTrace();
        }
        return new HashMap<>();
    }

    public static long getTimestamp() {
        Date date = new Date();
        return date.getTime();
    }

    public static String getRandomString(int length, int seed) {
        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random(seed);
        StringBuilder stringBuffer = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(62);
            stringBuffer.append(str.charAt(number));
        }
        return stringBuffer.toString().toUpperCase();
    }

    public static String getRandomString(int length) {
        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder stringBuffer = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(62);
            stringBuffer.append(str.charAt(number));
        }
        return stringBuffer.toString().toUpperCase();
    }

    public static String getRandomNumberString(int length) {
        Random random = new Random();
        StringBuilder stringBuffer = new StringBuilder();
        if (length < 1)
            return "0";
        stringBuffer.append(random.nextInt(9) + 1);
        for (int i = 1; i < length; i++)
            stringBuffer.append(random.nextInt(10));
        return stringBuffer.toString();
    }

    static public Integer getNumberFromString(String number) {
        String numberString = Pattern.compile("[^(0-9)]").matcher(number).replaceAll("").trim();
        return Integer.parseInt(numberString);
    }

    static public long getTimestampFromString(String time, String pattern) throws Exception {
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        format.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        Date data = format.parse(time);
        return data.getTime();
    }

    static public boolean available(String keyWord, Map<String, Object> map) {
        return map.containsKey(keyWord) && map.get(keyWord) != null && map.get(keyWord).toString().length() > 0;
    }
}
