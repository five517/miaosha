package com.mario.secondkill.utils;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;

@Component
public class MD5Util {
    public static String md5(String src) {
        return DigestUtils.md5Hex(src);
    }

    private static final String salt="hexiangdong";

    public static String inputPassToFromPass(String inputPass) {
        String str = "" + salt.charAt(0) + salt.charAt(1) + inputPass + salt.charAt(2) + salt.charAt(3);
        return md5(str);
    }

    public static String formPassToDBPass(String formPass, String salt) {
        String str = salt.charAt(0) + salt.charAt(1) + formPass + salt.charAt(2) + salt.charAt(3);
        return md5(str);
    }

    public static String inputPassDBPass(String inputPass, String salt) {
        String fromPass = inputPassToFromPass(inputPass);
        String dbPass = formPassToDBPass(fromPass, salt);
        return dbPass;
    }

    public static void main(String[] args) {
        //91b1e895ad031ed5dfac5f1273e40485
        System.out.println(inputPassToFromPass("123456"));
        System.out.println(formPassToDBPass("cf6473ca0856077cdd4fb2f9c4dfaeae",salt));
        System.out.println(inputPassDBPass("123456", salt));
    }
}
