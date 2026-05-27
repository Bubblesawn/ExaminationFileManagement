package com.exam.record.controller;

import com.exam.record.common.Result;
import com.exam.record.dto.MaterialTypeCreateDTO;
import com.exam.record.dto.MaterialTypeUpdateDTO;
import com.exam.record.entity.MaterialType;
import com.exam.record.service.MaterialTypeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @brief 材料类型维护接口。
 */
@RestController
@RequestMapping("/api/material-types")
public class MaterialTypeController {
    private final MaterialTypeService materialTypeService;

    /**
     * @brief 构造材料类型控制器。
     *
     * @param materialTypeService 材料类型维护业务服务。
     */
    public MaterialTypeController(MaterialTypeService materialTypeService) {
        this.materialTypeService = materialTypeService;
    }

    /**
     * @brief 查询材料类型列表。
     *
     * @param status 状态，可为空。
     * @return 材料类型列表。
     */
    @GetMapping
    public Result<List<MaterialType>> list(@RequestParam(required = false) String status) {
        return Result.success(materialTypeService.listTypes(status));
    }

    /**
     * @brief 新增材料类型。
     *
     * @param dto 材料类型新增请求对象。
     * @return 新增后的材料类型。
     */
    @PostMapping
    public Result<MaterialType> create(@Valid @RequestBody MaterialTypeCreateDTO dto) {
        return Result.success(materialTypeService.createType(dto));
    }

    /**
     * @brief 修改材料类型。
     *
     * @param id 材料类型ID。
     * @param dto 材料类型修改请求对象。
     * @return 修改后的材料类型。
     */
    @PutMapping("/{id}")
    public Result<MaterialType> update(@PathVariable Long id,
                                       @Valid @RequestBody MaterialTypeUpdateDTO dto) {
        return Result.success(materialTypeService.updateType(id, dto));
    }

    /**
     * @brief 删除单个材料类型。
     *
     * @param id 材料类型ID。
     * @return 无数据成功响应。
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        materialTypeService.deleteType(id);
        return Result.success();
    }
}
