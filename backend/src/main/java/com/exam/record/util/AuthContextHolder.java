package com.exam.record.util;

import com.exam.record.vo.TokenUserVO;

/**
 * @brief 当前请求登录用户上下文。
 *
 * @details
 * 拦截器校验 Token 后写入 ThreadLocal，业务代码可在同一请求线程中读取当前登录人。
 */
public final class AuthContextHolder {
    private static final ThreadLocal<TokenUserVO> USER_HOLDER = new ThreadLocal<>();

    private AuthContextHolder() {
    }

    /**
     * @brief 保存当前请求用户。
     *
     * @param user 当前登录用户。
     */
    public static void setUser(TokenUserVO user) {
        USER_HOLDER.set(user);
    }

    /**
     * @brief 获取当前请求用户。
     *
     * @return 当前登录用户，未登录时为 null。
     */
    public static TokenUserVO getUser() {
        return USER_HOLDER.get();
    }

    /**
     * @brief 清理当前请求用户。
     */
    public static void clear() {
        USER_HOLDER.remove();
    }
}
