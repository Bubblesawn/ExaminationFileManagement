package com.exam.record.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.record.common.BusinessException;
import com.exam.record.dto.MaterialTypeCreateDTO;
import com.exam.record.dto.MaterialTypeUpdateDTO;
import com.exam.record.entity.MaterialType;
import com.exam.record.mapper.MaterialTypeMapper;
import com.exam.record.service.MaterialTypeService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * @brief 材料类型维护业务实现。
 *
 * @details
 * 负责材料类型的新增、修改、查询和删除。类型编码在新增后保持稳定，
 * 用于关联已上传材料和后续算法分类结果。
 */
@Service
public class MaterialTypeServiceImpl extends ServiceImpl<MaterialTypeMapper, MaterialType>
        implements MaterialTypeService {
    private static final String STATUS_ENABLED = "ENABLED";
    private static final String STATUS_DISABLED = "DISABLED";

    /**
     * @brief 查询材料类型列表。
     *
     * @param status 状态，可为空。
     * @return 材料类型列表。
     */
    @Override
    public List<MaterialType> listTypes(String status) {
        validateOptionalStatus(status);
        LambdaQueryWrapper<MaterialType> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(MaterialType::getStatus, status);
        }
        wrapper.orderByAsc(MaterialType::getSortOrder).orderByDesc(MaterialType::getCreateTime);
        return list(wrapper);
    }

    /**
     * @brief 新增材料类型。
     *
     * @param dto 材料类型新增请求对象。
     * @return 新增后的材料类型。
     */
    @Override
    public MaterialType createType(MaterialTypeCreateDTO dto) {
        String typeCode = dto.getTypeCode().trim().toUpperCase();
        if (isTypeCodeExists(typeCode, null)) {
            throw new BusinessException(409, "材料类型编码已存在");
        }
        String status = StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : STATUS_ENABLED;
        validateStatus(status);
        MaterialType type = new MaterialType();
        type.setTypeCode(typeCode);
        type.setTypeName(dto.getTypeName());
        type.setDescription(dto.getDescription());
        type.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        type.setStatus(status);
        save(type);
        return getById(type.getId());
    }

    /**
     * @brief 修改材料类型。
     *
     * @param id 材料类型ID。
     * @param dto 材料类型修改请求对象。
     * @return 修改后的材料类型。
     */
    @Override
    public MaterialType updateType(Long id, MaterialTypeUpdateDTO dto) {
        MaterialType type = getExistingType(id);
        String status = StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : STATUS_ENABLED;
        validateStatus(status);
        type.setTypeName(dto.getTypeName());
        type.setDescription(dto.getDescription());
        type.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        type.setStatus(status);
        updateById(type);
        return getById(id);
    }

    /**
     * @brief 删除单个材料类型。
     *
     * @param id 材料类型ID。
     */
    @Override
    public void deleteType(Long id) {
        getExistingType(id);
        removeById(id);
    }

    private MaterialType getExistingType(Long id) {
        MaterialType type = getById(id);
        if (type == null) {
            throw new BusinessException(404, "材料类型不存在");
        }
        return type;
    }

    private boolean isTypeCodeExists(String typeCode, Long excludeId) {
        LambdaQueryWrapper<MaterialType> wrapper = new LambdaQueryWrapper<MaterialType>()
                .eq(MaterialType::getTypeCode, typeCode);
        if (excludeId != null) {
            wrapper.ne(MaterialType::getId, excludeId);
        }
        return count(wrapper) > 0;
    }

    private void validateOptionalStatus(String status) {
        if (StringUtils.hasText(status)) {
            validateStatus(status);
        }
    }

    private void validateStatus(String status) {
        if (!STATUS_ENABLED.equals(status) && !STATUS_DISABLED.equals(status)) {
            throw new BusinessException(400, "材料类型状态只能为ENABLED或DISABLED");
        }
    }
}
