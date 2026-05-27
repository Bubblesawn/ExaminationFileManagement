package com.exam.record.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * @brief 系统菜单排序请求对象。
 */
@Data
public class SysMenuSortDTO {
    @Min(value = 0, message = "不能小于0")
    private Long parentId;

    @Min(value = 0, message = "不能小于0")
    private Integer menuSort;
}
