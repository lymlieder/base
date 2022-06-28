package com.example.demo.baseFunction;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.tomcat.util.codec.binary.Base64;
import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jcajce.provider.digest.SHA1;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.util.Base64Utils;
//import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

@Log4j2
public class Crypto {
    static public String MD5(String input) {
        return DigestUtils.md5Hex(input.getBytes());
    }

    static public String MD5_withSalt(String input, String salt) {
        return MD5(input + "_" + salt);
    }

    private static final String KEY_ALGORITHM = "AES";
    private static final String DEFAULT_CIPHER_ALGORITHM = "AES/CBC/PKCS7Padding";//默认的加密算法

    static {
        try {
            Security.addProvider(new BouncyCastleProvider());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 加解密密钥, 外部可以
     */
    public static final String AES_DATA_SECURITY_KEY = "4%YkW!@g5LGcf9Ut";
    /**
     * 算法/加密模式/填充方式
     */
    private static final String AES_PKCS5P = "AES/ECB/PKCS7Padding";

    private static final String AES_PERSON_KEY_SECURITY_KEY = "pisnyMyZYXuCNcRd";

    /**
     * 加密
     *
     * @param str 需要加密的字符串
     * @param key 密钥
     * @return 密文
     */
    public static String encrypt(String str, String key) throws Exception {
        if (str == null)
            return null;
        if (key == null)
            throw new RuntimeException("key不能为空");
        key = MD5(key).substring(0, 16);
        // 判断Key是否为16位
        byte[] raw = key.getBytes(StandardCharsets.UTF_8);
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        // "算法/模式/补码方式"
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
        IvParameterSpec iv = new IvParameterSpec("0102030405060708".getBytes());
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
        byte[] encrypted = cipher.doFinal(str.getBytes(StandardCharsets.UTF_8));
        // 此处使用BASE64做转码功能，同时能起到2次加密的作用。
//            return new BASE64Encoder().encode(encrypted);
//            return Array.getByte() encrypted;
        return printHexString(encrypted);
//            return encodeBase64(encrypted, false);
    }

    // 解密
    public static String decrypt(String str, String key) throws Exception {
        if (str == null)
            return null;
        if (key == null)
            throw new RuntimeException("key不能为空");
//        key = MD5(key).substring(0, 16);
        byte[] raw = key.getBytes(StandardCharsets.UTF_8);
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
        IvParameterSpec iv = new IvParameterSpec("0102030405060708".getBytes());
        cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
        byte[] encrypted = new Base64().decode(str);// 先用base64解密
        byte[] original = cipher.doFinal(encrypted);
        return new String(original, StandardCharsets.UTF_8);
    }

    public static String printHexString(byte[] b) {
        StringBuilder res = new StringBuilder();
        for (byte value : b) {
            String hex = Integer.toHexString(value & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            res.append(hex);
        }
        return res.toString();
    }

    /**
     * AES 加密操作
     *
     * @param text 待加密内容
     * @param key  加密密钥
     * @return 返回Base64转码后的加密数据
     */
    public static String encryptAES(String text, String key) {
        try {
            Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);// 创建密码器

            byte[] byteContent = text.getBytes(StandardCharsets.UTF_8);

            IvParameterSpec iv = new IvParameterSpec("0102030405060708".getBytes());
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(key), iv);// 初始化为加密模式的密码器

            byte[] result = cipher.doFinal(byteContent);// 加密

            return new String(Base64Utils.encode(result));//通过Base64转码返回

        } catch (Exception e) {
            e.printStackTrace();
            log.error(e);
        }

        return null;
    }

    /**
     * AES 解密操作
     *
     * @param content 密文
     * @param key     密码
     * @return 明文
     */
    public static String decryptAES(String content, String key) {

        try {
            //实例化
            Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);

            //使用密钥初始化，设置为解密模式
            IvParameterSpec iv = new IvParameterSpec("0102030405060708".getBytes());
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(key), iv);

            //执行操作
            byte[] result = cipher.doFinal(Base64Utils.decode(content.getBytes()));
            return new String(result, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e);
        }

        return null;
    }

    /**
     * 生成加密秘钥
     *
     * @return 生成结果
     */
    private static SecretKeySpec getSecretKey(final String key) {
        //返回生成指定算法密钥生成器的 KeyGenerator 对象
        KeyGenerator kg;

        try {
            kg = KeyGenerator.getInstance(KEY_ALGORITHM);

            //AES 要求密钥长度为 128
            kg.init(128, new SecureRandom(key.getBytes()));

            //生成一个密钥
            SecretKey secretKey = kg.generateKey();

            return new SecretKeySpec(secretKey.getEncoded(), KEY_ALGORITHM);// 转换为AES专用密钥
        } catch (Exception e) {
            log.error(e);
        }

        return null;
    }

    static public String encodeBase64(byte[] data, boolean urlSafe) {
        byte[] encodedByte = Base64.encodeBase64(data);
        String result = new String(encodedByte, StandardCharsets.UTF_8);
        if (urlSafe)
            result = result.replaceAll("/+", "-").replaceAll("/", "_").replaceAll("=", "");
        return result;
    }

    static public String encodeBase64(String data, boolean urlSafe) {
        byte[] encodedByte = Base64.encodeBase64(data.getBytes(StandardCharsets.UTF_8));
        String result = new String(encodedByte, StandardCharsets.UTF_8);
        if (urlSafe)
            result = result.replaceAll("/+", "-").replaceAll("/", "_").replaceAll("=", "");
        return result;
    }

    /**
     * 安全Base64加密
     */
    public static String encodeBigBase64(String data) {
        byte[] encodedByte = Base64.encodeBase64(data.getBytes(StandardCharsets.UTF_8), true);
        return new String(encodedByte, StandardCharsets.UTF_8);
    }

    /**
     * Base64解密
     */
    public static String decodeBase64(String data) {
        byte[] decodedByte = Base64.decodeBase64(data.getBytes(StandardCharsets.UTF_8));
        return new String(decodedByte, StandardCharsets.UTF_8);
    }

    /**
     * 通过密钥进行加密
     * 指定密钥进行加密
     *
     * @param key     密钥
     * @param srcData 被加密的byte数组
     * @return
     */
    public static byte[] encryptSM3(String key, String srcData) {
        KeyParameter keyParameter = new KeyParameter(key.getBytes());
        SM3Digest digest = new SM3Digest();
        HMac mac = new HMac(digest);
        mac.init(keyParameter);
        mac.update(srcData.getBytes(), 0, srcData.length());
        byte[] result = new byte[mac.getMacSize()];
        mac.doFinal(result, 0);
        return result;
    }

    public static String sha1(String input) {
        return DigestUtils.sha1Hex(input);
    }

    public static byte[] SM3(byte[] srcData) {
        SM3Digest digest = new SM3Digest();
        digest.update(srcData, 0, srcData.length);
        byte[] hash = new byte[digest.getDigestSize()];
        digest.doFinal(hash, 0);
        return hash;
    }

    public static String byteToHexString(byte ib) {
        char[] Digit = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        char[] ob = new char[2];
        ob[0] = Digit[(ib >>> 4) & 0X0f];
        ob[1] = Digit[ib & 0X0F];
        return new String(ob);
    }

    static public String getSortedStrings(Map<String, String> paramMap) {
        Map<String, Object> map = new HashMap<>(paramMap);
        return getSortedString(map);
    }

    static public String getSortedString(Map<String, Object> paramMap) {
        SortedMap<String, Object> newMap = new TreeMap<>(paramMap);
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, Object> param : newMap.entrySet())
            result.append(param.getKey()).append("=").append(param.getValue().toString()).append("&");
        result.deleteCharAt(result.length() - 1);
        return result.toString();
    }

    /**
     * SM4
     */
    static public String encryptMS4(String text, String key) throws Exception {
        byte[] result = encrypt_Ecb_Padding(Base64.decodeBase64(key.getBytes()), text.getBytes(StandardCharsets.UTF_8));
        return new String(Base64.encodeBase64(result), StandardCharsets.UTF_8);
    }

    static public String decryptMS4(String text, String key) throws Exception {
        byte[] result = decrypt_Ecb_Padding(Base64.decodeBase64(key.getBytes()), Base64.decodeBase64(text.getBytes()));
        return new String(result, StandardCharsets.UTF_8);
    }

    static byte[] encrypt_Ecb_Padding(byte[] key, byte[] data)
            throws InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException,
            NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = generateEcbCipher(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    static byte[] decrypt_Ecb_Padding(byte[] key, byte[] cipherText)
            throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException,
            NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException {
        Cipher cipher = generateEcbCipher(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(cipherText);
    }

    private static Cipher generateEcbCipher(int mode, byte[] key)
            throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException,
            InvalidKeyException {
        Cipher cipher = Cipher.getInstance("SM4/ECB/PKCS7Padding", BouncyCastleProvider.PROVIDER_NAME);
        Key sm4Key = new SecretKeySpec(key, "SM4");
        cipher.init(mode, sm4Key);
        return cipher;
    }

    public static byte[] generateKey_MS4() throws NoSuchAlgorithmException, NoSuchProviderException {
        return generateKey_MS4(128);
    }

    public static byte[] generateKey_MS4(int keySize) throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyGenerator kg = KeyGenerator.getInstance("SM4", BouncyCastleProvider.PROVIDER_NAME);
        kg.init(keySize, new SecureRandom());
        return kg.generateKey().getEncoded();
    }

}
