package com.exam.record.service;

import com.exam.record.dto.MaterialTypeCreateDTO;
import com.exam.record.dto.MaterialTypeUpdateDTO;
import com.exam.record.entity.MaterialType;

import java.util.List;

/**
 * @brief 材料类型维护业务接口。
 */
public interface MaterialTypeService {
    /**
     * @brief 查询材料类型列表。
     *
     * @param status 状态，可为空。
     * @return 材料类型列表。
     */
    List<MaterialType> listTypes(String status);

    /**
     * @brief 新增材料类型。
     *
     * @param dto 材料类型新增请求对象。
     * @return 新增后的材料类型。
     */
    MaterialType createType(MaterialTypeCreateDTO dto);

    /**
     * @brief 修改材料类型。
     *
     * @param id 材料类型ID。
     * @param dto 材料类型修改请求对象。
     * @return 修改后的材料类型。
     */
    MaterialType updateType(Long id, MaterialTypeUpdateDTO dto);

    /**
     * @brief 删除单个材料类型。
     *
     * @param id 材料类型ID。
     */
    void deleteType(Long id);
}
