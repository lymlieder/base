package com.example.demo.baseFunction;

import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.google.gson.Gson;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Log4j2
//@RestController
public class PhoneMessage {

    static Gson gson = new Gson();

    static String RegionId,// "cn-hangzhou";
            SignName = "开心麻花",//"";
            TemplateCode,//"";
            accessKeyId,//"LTAI4GAEkHingTg1cuC6e7gi";
            accessSecret;//"LdNaG6MzZjMspIs4by3tQbmJoe2AQp";

    public PhoneMessage() {
        RegionId = Config.getConfig("message.RegionId");
//        SignName = Config.getConfig("message.SignName");
        TemplateCode = Config.getConfig("message.TemplateCode");
        accessKeyId = Config.getConfig("message.accessKeyId");
        accessSecret = Config.getConfig("message.accessSecret");
    }

    static public void sendMessage(String PhoneNumbers, Map<String, Object> messageMap) throws Exception {
        sendMessage(PhoneNumbers, messageMap, TemplateCode);
    }

    static public void sendMessage(String PhoneNumbers, Map<String, Object> messageMap, String templateCode) throws Exception {
        DefaultProfile profile = DefaultProfile.getProfile(RegionId, accessKeyId, accessSecret);
        IAcsClient client = new DefaultAcsClient(profile);

        CommonRequest request = new CommonRequest();
        request.setSysMethod(MethodType.POST);
        request.setSysDomain("dysmsapi.aliyuncs.com");
        request.setSysVersion("2017-05-25");
        request.setSysAction("SendSms");
        request.putQueryParameter("RegionId", RegionId);
        request.putQueryParameter("PhoneNumbers", PhoneNumbers);
        request.putQueryParameter("SignName", SignName);
        request.putQueryParameter("TemplateCode", templateCode);
        if (messageMap != null)
            request.putQueryParameter("TemplateParam", gson.toJson(messageMap));
        CommonResponse response = client.getCommonResponse(request);
        log.info(response.getData());
    }
}
