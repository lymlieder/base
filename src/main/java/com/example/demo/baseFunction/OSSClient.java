package com.example.demo.baseFunction;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.*;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

//@RestController
public class OSSClient {
    // Endpoint以杭州为例，其它Region请按实际情况填写。
    static String endpoint;
    static String urlHead;
    // 阿里云主账号AccessKey拥有所有API的访问权限，风险很高。强烈建议您创建并使用RAM账号进行API访问或日常运维，请登录 https://ram.console.aliyun.com 创建RAM账号。
    String accessKeyId;
    String accessKeySecret;
    static String bucketName;
    // 创建OSSClient实例。
    OSS ossClient;

    public OSSClient() {
        endpoint = Config.getConfig("OSS.endpoint");
        urlHead = Config.getConfig("OSS.urlHead");
        accessKeyId = Config.getConfig("OSS.accessKeyId");
        accessKeySecret = Config.getConfig("OSS.accessKeySecret");
        bucketName = Config.getConfig("OSS.bucketName");
        ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }

    public boolean contain(String url) {
        return ossClient.doesObjectExist(bucketName, getFileName(url));
    }

    public void delete(String url) {
        ossClient.deleteObject(bucketName, getFileName(url));
    }

    String getFileName(String url) {
        if (url.contains(urlHead))
            return url.split(urlHead + "/")[1];
        return url.contains(endpoint) ? url.split(endpoint + "/")[1] : url;
    }

    static public String getFullUrl(String purePath) {
        String base64 = URLEncoder.encode(purePath, StandardCharsets.UTF_8)
                .replaceAll("%2F", "/");
        return "http://" + (bucketName + "." + endpoint + "/" + base64).replace("//", "/");
    }

    //上传字符串
    public String upload(String purePath, String content) {
        return upload(purePath, content, null);
    }

    //上传字符串
    public String upload(String purePath, String content, Callback callback) {
        // <yourObjectName> 表示上传文件到OSS时需要指定包含文件后缀在内的完整路径，例如abc / efg / 123. jpg。
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, purePath, new ByteArrayInputStream(content.getBytes()));
        // 如果需要上传时设置存储类型与访问权限，请参考以下示例代码。
//        ObjectMetadata metadata = new ObjectMetadata();
//        metadata.setHeader(OSSHeaders.OSS_STORAGE_CLASS, StorageClass.Standard.toString());
//        metadata.setObjectAcl(CannedAccessControlList.PublicRead);
//        putObjectRequest.setMetadata(metadata);
        if (callback != null)
            putObjectRequest.setCallback(callback);
        // 上传字符串。
        ossClient.putObject(putObjectRequest);
        //http://kaixinkaima-test.oss-cn-shanghai.aliyuncs.com/test/test.txt
        return getFullUrl(purePath);
    }

    //上传流
    public String upload(String purePath, InputStream inputStream) {
        return upload(purePath, inputStream, null);
    }

    //上传流
    public String upload(String purePath, InputStream inputStream, Callback callback) {
        // 上传网络流。
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, purePath, inputStream);
        if (callback != null)
            putObjectRequest.setCallback(callback);
        ossClient.putObject(putObjectRequest);
        return getFullUrl(purePath);
    }

    //上传文件
    public String upload(String purePath, File file) {
        return upload(purePath, file, null);
    }

    //上传文件
    public String upload(String purePath, File file, Callback callback) {
        // 创建PutObjectRequest对象。
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, purePath, file);
        // 如果需要上传时设置存储类型与访问权限，请参考以下示例代码。
//        ObjectMetadata metadata = new ObjectMetadata();
//        metadata.setHeader(OSSHeaders.OSS_STORAGE_CLASS, StorageClass.Standard.toString());
//        metadata.setObjectAcl(CannedAccessControlList.PublicRead);
//        putObjectRequest.setMetadata(metadata);
        if (callback != null)
            putObjectRequest.setCallback(callback);
        // 上传文件。
        ossClient.putObject(putObjectRequest);
        return getFullUrl(purePath);
    }

    public InputStream downloadStream(String url) {
//        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        // ossObject包含文件所在的存储空间名称、文件名称、文件元信息以及一个输入流。
        String key = getFileName(url);
        OSSObject ossObject = ossClient.getObject(bucketName, key);
        // 读取文件内容。
        return ossObject.getObjectContent();
    }

    public void downloadFile(String url, String localPath) {
        // 下载OSS文件到本地文件。如果指定的本地文件存在会覆盖，不存在则新建。
        ossClient.getObject(new GetObjectRequest(bucketName, getFileName(url)), new File(localPath));
    }
}
