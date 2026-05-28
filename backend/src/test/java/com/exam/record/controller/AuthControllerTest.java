package com.exam.record.controller;

import com.exam.record.common.BusinessException;
import com.exam.record.common.GlobalExceptionHandler;
import com.exam.record.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * @brief 登录认证控制器回归测试。
 */
class AuthControllerTest {

    /**
     * @brief 校验未携带 Authorization 请求头时返回统一 401 响应。
     *
     * @throws Exception MockMvc 请求执行异常。
     */
    @Test
    void meWithoutAuthorizationReturnsUnauthorizedResult() throws Exception {
        AuthService authService = mock(AuthService.class);
        when(authService.verifyToken(isNull())).thenThrow(new BusinessException(401, "未提供登录凭证"));
        MockMvc mockMvc = standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("未提供登录凭证"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }
}
