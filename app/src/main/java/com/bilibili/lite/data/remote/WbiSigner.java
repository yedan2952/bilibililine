package com.bilibili.lite.data.remote;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

public class WbiSigner {

    private static final String MIXIN_KEY_EMPTY = "ea1db124afe347a1";

    public static Map<String, String> sign(Map<String, String> params) {
        TreeMap<String, String> sorted = new TreeMap<>(params);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            sb.append(e.getKey()).append("=").append(e.getValue()).append("&");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append(MIXIN_KEY_EMPTY);
        String wts = String.valueOf(System.currentTimeMillis() / 1000);
        sorted.put("wts", wts);
        sorted.put("w_rid", md5(sb.toString()));
        return sorted;
    }

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }
}
