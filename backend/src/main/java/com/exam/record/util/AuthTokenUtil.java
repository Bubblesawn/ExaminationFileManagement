package com.exam.record.util;

import com.exam.record.common.BusinessException;
import com.exam.record.entity.SysUser;
import com.exam.record.vo.TokenUserVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * @brief 认证 Token 生成和校验工具。
 *
 * @details
 * Token 由 Base64Url 编码载荷和 HMAC-SHA256 签名组成，载荷包含用户标识、账号、
 * 姓名、过期时间和随机串。服务端校验签名与过期时间后恢复登录用户上下文。
 */
@Component
public class AuthTokenUtil {
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SEPARATOR = ".";
    private static final String PAYLOAD_SEPARATOR = "\n";

    @Value("${auth.token.secret:exam-record-default-secret-change-me}")
    private String secret;

    @Value("${auth.token.expire-seconds:7200}")
    private long expireSeconds;

    /**
     * @brief 为登录成功用户生成访问 Token。
     *
     * @param user 系统用户。
     * @return 访问 Token。
     */
    public String generateToken(SysUser user) {
        long expiresAt = Instant.now().getEpochSecond() + expireSeconds;
        String payload = String.join(PAYLOAD_SEPARATOR,
                String.valueOf(user.getId()),
                user.getUsername(),
                safe(user.getRealName()),
                String.valueOf(expiresAt),
                UUID.randomUUID().toString());
        String encodedPayload = encode(payload.getBytes(StandardCharsets.UTF_8));
        return encodedPayload + SEPARATOR + sign(encodedPayload);
    }

    /**
     * @brief 解析并校验访问 Token。
     *
     * @param token 访问 Token。
     * @return Token 中的登录用户信息。
     */
    public TokenUserVO parseToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(401, "未提供登录凭证");
        }
        String[] parts = token.split("\\.", 2);
        if (parts.length != 2 || !sign(parts[0]).equals(parts[1])) {
            throw new BusinessException(401, "登录凭证无效");
        }
        try {
            String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            String[] values = payload.split(PAYLOAD_SEPARATOR, -1);
            if (values.length < 5) {
                throw new BusinessException(401, "登录凭证格式错误");
            }
            long expiresAt = Long.parseLong(values[3]);
            if (expiresAt < Instant.now().getEpochSecond()) {
                throw new BusinessException(401, "登录凭证已过期");
            }
            return new TokenUserVO(Long.valueOf(values[0]), values[1], values[2]);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(401, "登录凭证格式错误");
        }
    }

    /**
     * @brief 获取 Token 有效期秒数。
     *
     * @return 有效期秒数。
     */
    public long getExpireSeconds() {
        return expireSeconds;
    }

    private String sign(String content) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return encode(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Token 签名失败", exception);
        }
    }

    private String encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
