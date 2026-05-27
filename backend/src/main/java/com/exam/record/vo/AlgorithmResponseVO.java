package com.exam.record.vo;

import lombok.Data;

import java.util.Map;

/**
 * @brief 算法服务统一响应视图对象。
 */
@Data
public class AlgorithmResponseVO {
    private Integer code;
    private String message;
    private Map<String, Object> data;
}
