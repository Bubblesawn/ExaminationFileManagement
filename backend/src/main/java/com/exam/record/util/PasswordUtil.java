package com.exam.record.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.util.StringUtils;

/**
 * @brief 用户密码哈希与校验工具。
 *
 * @details
 * 系统新写入的密码统一使用 {sha256} 前缀保存 SHA-256 摘要；登录校验继续兼容
 * 初始化测试数据中的 {noop} 明文占位，便于历史数据平滑迁移。
 */
public final class PasswordUtil {
    private static final String NOOP_PREFIX = "{noop}";
    private static final String SHA256_PREFIX = "{sha256}";

    private PasswordUtil() {
    }

    /**
     * @brief 生成可持久化的密码哈希。
     *
     * @param rawPassword 用户输入的明文密码。
     * @return 带算法前缀的密码哈希。
     */
    public static String encode(String rawPassword) {
        return SHA256_PREFIX + sha256(rawPassword);
    }

    /**
     * @brief 校验明文密码是否匹配数据库密码。
     *
     * @param rawPassword 用户输入的明文密码。
     * @param storedPassword 数据库存储的密码值。
     * @return 密码是否匹配。
     */
    public static boolean matches(String rawPassword, String storedPassword) {
        if (!StringUtils.hasText(rawPassword) || !StringUtils.hasText(storedPassword)) {
            return false;
        }
        if (storedPassword.startsWith(NOOP_PREFIX)) {
            return rawPassword.equals(storedPassword.substring(NOOP_PREFIX.length()));
        }
        if (storedPassword.startsWith(SHA256_PREFIX)) {
            return sha256(rawPassword).equalsIgnoreCase(storedPassword.substring(SHA256_PREFIX.length()));
        }
        return rawPassword.equals(storedPassword);
    }

    private static String sha256(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("密码摘要算法不可用", exception);
        }
    }
}
