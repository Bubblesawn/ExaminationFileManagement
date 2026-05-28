package com.exam.record.service.impl;

import com.exam.record.common.BusinessException;
import com.exam.record.util.AuthTokenUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @brief 登录认证服务回归测试。
 */
class AuthServiceImplTest {

    /**
     * @brief 校验空 Token 不会触发退出 Token 集合的空指针异常。
     */
    @Test
    void verifyTokenWithNullDelegatesToTokenParser() {
        AuthTokenUtil authTokenUtil = mock(AuthTokenUtil.class);
        when(authTokenUtil.parseToken(null)).thenThrow(new BusinessException(401, "未提供登录凭证"));
        AuthServiceImpl authService = new AuthServiceImpl(null, null, authTokenUtil, null);

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.verifyToken(null));

        assertEquals(401, exception.getCode());
        assertEquals("未提供登录凭证", exception.getMessage());
    }
}
