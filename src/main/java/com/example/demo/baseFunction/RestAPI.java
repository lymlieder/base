package com.example.demo.baseFunction;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.log4j.Log4j2;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Log4j2
public class RestAPI {
    static Gson gson = new Gson();
    static ObjectMapper json = new ObjectMapper();

    /**
     * 通用GET函数
     *
     * @param urlStr url，不带键值对
     * @param params 参数Map
     * @return 访问返回值
     */
    public static Map<String, Object> get(String urlStr, Map<String, Object> params) {
        return get(urlStr, params, false);
    }

    public static Map<String, Object> get(String urlStr, Map<String, Object> params, boolean withParseJson) {
        return get(urlStr, params, withParseJson, null);
    }

    /**
     * 根据body生成GET中带键值对的url，目的是和POST保持调用格式一致
     *
     * @param url  原url
     * @param body 参数Map
     * @return 生成url
     */
    public static String getGETUrlFromBody(String url, Map<String, ?> body) {
        StringBuilder s = new StringBuilder(url + "?");
        SortedMap<String, ?> newMap = new TreeMap<>(body);
        for (Map.Entry<String, ?> data : newMap.entrySet())
            if (data.getValue() != null)
                s.append(data.getKey()).append("=").append(URLEncoder.encode(data.getValue().toString(), StandardCharsets.UTF_8)).append("&");
        return s.substring(0, s.length() - 1);
    }

    public static Map<String, Object> get(String urlStr, Map<String, Object> params, boolean withParseJson, HttpHeaders headers) {
        // https://attacomsian.com/blog/spring-boot-resttemplate-get-request-parameters-headers

        if (params != null && !params.isEmpty())
            urlStr = getGETUrlFromBody(urlStr, params);
        RestTemplate restTemplate = new RestTemplate();

        if (headers == null)
            headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<?> request = new HttpEntity<>(headers);

        try {
            restTemplate.getMessageConverters().add(1, new StringHttpMessageConverter(StandardCharsets.UTF_8));
            ResponseEntity<String> response = restTemplate.exchange(urlStr, HttpMethod.GET, request, String.class, 1);

            Map<String, Object> result = new HashMap<>();
            result.put("code", response.getStatusCode());
            if (withParseJson) {
                Object data;
                try {
                    data = json.readValue(response.getBody(), new TypeReference<>() {
                    });
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                    data = gson.fromJson(response.getBody(), new TypeToken<>() {
                    }.getType());
                }
                result.put("data", data);
            } else
                result.put("data", response.getBody());
            return result;
        } catch (HttpClientErrorException e) {
            Map<String, Object> result = new HashMap<>();
            Object data = gson.fromJson(e.getResponseBodyAsString(), new TypeToken<>() {
            }.getType());
            result.put("code", e.getStatusCode());
            result.put("data", data);
            return result;
        }
    }

    /**
     * 通用POST函数
     *
     * @param urlStr       url，不带键值对
     * @param request_body 参数Map
     * @return 访问返回值
     */
    public static Map<String, Object> post(String urlStr, Map<String, Object> request_body) {
        return post(urlStr, request_body, -1);
    }

    public static Map<String, Object> post(String urlStr, Map<String, Object> request_body, int timeout) {
        return post(urlStr, request_body, false, false, timeout);
    }

    public static Map<String, Object> post(String urlStr, Map<String, Object> request_body, boolean allowNullMap, boolean withParseJson, int timeout) {
        return post(urlStr, request_body, allowNullMap, withParseJson, timeout, null);
    }

    public static Map<String, Object> post(String urlStr, Map<String, Object> request_body, boolean withParseJson, HttpHeaders headers) {
        return post(urlStr, request_body, false, withParseJson, -1, headers);
    }

    public static Map<String, Object> post(String urlStr, Map<String, Object> request_body, boolean withParseJson, int timeout, HttpHeaders headers) {
        return post(urlStr, request_body, false, withParseJson, timeout, headers);
    }

    public static Map<String, Object> post(String urlStr, Map<String, Object> request_body, boolean allowNullMap, boolean withParseJson, int timeout, HttpHeaders headers) {
        if ((request_body == null || request_body.isEmpty()) && !allowNullMap) {
            request_body = new HashMap<>();
            request_body.put("", "");
        }

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        if (timeout > -1) {
            requestFactory.setConnectTimeout(timeout);
            requestFactory.setReadTimeout(timeout);
        }
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        if (headers == null)
            headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.parseMediaType("application/json; charset=UTF-8"));
        HttpEntity<Map<String, Object>> formEntity = new HttpEntity<>(request_body, headers);

        try {
            restTemplate.getMessageConverters().add(1, new StringHttpMessageConverter(StandardCharsets.UTF_8));
            ResponseEntity<String> response = restTemplate.postForEntity(urlStr, formEntity, String.class); // 提交的body内容为user对象，请求的返回的body类型为String

            Map<String, Object> result = new HashMap<>();
            result.put("code", response.getStatusCode());
            if (withParseJson) {
                Object data;
                try {
                    data = json.readValue(response.getBody(), new TypeReference<>() {
                    });
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                    data = gson.fromJson(response.getBody(), new TypeToken<>() {
                    }.getType());
                }
                result.put("data", data);
            } else
                result.put("data", response.getBody());
            return result;
        } catch (HttpClientErrorException e) {
            Map<String, Object> result = new HashMap<>();
            Object data = gson.fromJson(e.getResponseBodyAsString(), new TypeToken<>() {
            }.getType());
            result.put("code", e.getStatusCode());
            result.put("data", data);
            return result;
        }
    }

    static public String postStream(String url, String json, String fileName) throws Exception {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(url);
        httpPost.addHeader("Content-type", "application/json; charset=UTF-8");
        httpPost.setHeader("Accept", "application/json");
        StringEntity s = new StringEntity(json, StandardCharsets.UTF_8);
        s.setContentType("UTF-8");
        httpPost.setEntity(s);
        HttpResponse response = httpClient.execute(httpPost);
        if (response.getStatusLine().getStatusCode() == org.apache.http.HttpStatus.SC_OK) {
            org.apache.http.HttpEntity entity = response.getEntity();
            InputStream inputStream = entity.getContent();
            return FileManager.uploadFile(inputStream, "postStream", fileName);
        } else
            throw new Exception(response.getEntity().getContent().toString());
    }

    public static Map<String, Object> postForm(String urlStr, Map<String, Object> request_body, boolean ifNeedParseJson, HttpHeaders headers) {
        if (request_body == null || request_body.isEmpty()) {
            request_body = new HashMap<>();
            request_body.put("", "");
        }

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        for (Map.Entry<String, Object> data : request_body.entrySet())
            body.add(data.getKey(), data.getValue());

        if (headers == null)
            headers = new HttpHeaders();
        headers.setContentType(new MediaType(MediaType.MULTIPART_FORM_DATA, StandardCharsets.UTF_8));
        HttpEntity<MultiValueMap<String, Object>> formEntity = new HttpEntity<>(body, headers);

        HttpClient httpClient = HttpClientBuilder.create()
                .setRedirectStrategy(new LaxRedirectStrategy())
                .build();
        HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        RestTemplate restTemplate = new RestTemplate(httpRequestFactory);
        restTemplate.getMessageConverters().set(1, new StringHttpMessageConverter(StandardCharsets.UTF_8));

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(urlStr, formEntity, String.class); // 提交的body内容为user对象，请求的返回的body类型为String

            Map<String, Object> result = new HashMap<>();
            result.put("code", response.getStatusCode());
            if (ifNeedParseJson) {
                Object data = gson.fromJson(response.getBody(), new TypeToken<>() {
                }.getType());
                result.put("data", data);
            } else
                result.put("data", response.getBody());
            return result;
        } catch (HttpClientErrorException e) {
            Map<String, Object> result = new HashMap<>();
            Object data = gson.fromJson(e.getResponseBodyAsString(), new TypeToken<>() {
            }.getType());
            result.put("code", e.getStatusCode());
            result.put("data", data);
            return result;
        }
    }
}