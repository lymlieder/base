package com.example.demo.baseFunction;

import com.example.demo.CommonFunction;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

@Log4j2
public class FileManager {

    //    static OSSClient ossClient = new OSSClient();
    static boolean k8sMode = Environment.getK8sMode();

    static public String getPathInEnvironment(String path) {
        return k8sMode ? path.replace("./", "") : path;
    }

    static public void uploadFile(ResResult resResult, MultipartFile file, String ossSubPath, String name) {
        try {
            String url = FileManager.uploadFile(file, ossSubPath, name);
            Map<String, String> result = Collections.singletonMap("url", url);
            resResult.setSuccess(result);
        } catch (Exception e) {
            resResult.setFail(ResResult.ResultCode.OPERATE_FAIL);
            e.printStackTrace();
            log.error(e);
        }
    }

    static public String uploadFile(MultipartFile file, String ossSubPath, String name) throws Exception {
//        long timestamp = CommonFunction.getTimestamp();
//        String tempPath = FileManager.getPathInEnvironment("./temp/" + file.getOriginalFilename() + timestamp);
        String ossPartPath = (ossSubPath.endsWith("/") ? ossSubPath : ossSubPath + "/") + (name != null && name.length() > 0 ? name : file.getOriginalFilename());
//        File tempFile = new File(tempPath);
//        FileOutputStream out = new FileOutputStream(tempFile);
//        out.write(file.getBytes());
//        out.flush();
//        out.close();
//        String url = ClientPool.ossClient.upload(ossPartPath, tempFile);
//        tempFile.delete();

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(file.getBytes());
        String url = ClientPool.ossClient.upload(ossPartPath, byteArrayInputStream);
//        boolean exist = ossClient.contain(url);
//        ossClient.delete(url);
        return url;
    }

    static public String uploadFile(InputStream inputStream, String ossSubPath, String name) {
        long timestamp = CommonFunction.getTimestamp();
        String ossPartPath = (ossSubPath.endsWith("/") ? ossSubPath : ossSubPath + "/") + (name != null && name.length() > 0 ? name : timestamp);
        return ClientPool.ossClient.upload(ossPartPath, inputStream);
    }

    static public void updateFile(ResResult resResult, String oldUrl, MultipartFile newFile, String newOssPath, String newName) {
        try {
            String url = FileManager.updateFile(oldUrl, newFile, newOssPath, newName);
            Map<String, String> result = Collections.singletonMap("newUrl", url);
            resResult.setSuccess(result);
        } catch (Exception e) {
            resResult.setFail(ResResult.ResultCode.OPERATE_FAIL);
            e.printStackTrace();
            log.error(e);
        }
    }

    static public String updateFile(String oldUrl, MultipartFile newFile, String newOssPath, String newName) throws Exception {
        String result = uploadFile(newFile, newOssPath, newName);
        if (!ClientPool.ossClient.contain(newOssPath))
            throw new Exception("File '" + newOssPath + "' uploaded fail.");
        if (oldUrl.contains(ClientPool.ossClient.endpoint) && result.contains(ClientPool.ossClient.endpoint) &&
                !Objects.equals(result, oldUrl) && ClientPool.ossClient.contain(oldUrl))
            ClientPool.ossClient.delete(oldUrl);
        return result;
    }

    static public InputStream getOSSFile(String url) {
        return ClientPool.ossClient.downloadStream(url);
    }

    static public String getOSSFileString(String url) throws Exception {
        InputStreamReader read = new InputStreamReader(getOSSFile(url));
        BufferedReader br = new BufferedReader(read);
        StringBuilder result = new StringBuilder();
        String data;
        while ((data = br.readLine()) != null)
            result.append(data);
        return result.toString();
    }
}
