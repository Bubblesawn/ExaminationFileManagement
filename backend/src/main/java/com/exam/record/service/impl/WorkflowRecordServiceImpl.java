package com.exam.record.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.record.common.BusinessException;
import com.exam.record.entity.AuditRecord;
import com.exam.record.mapper.AuditRecordMapper;
import com.exam.record.service.WorkflowRecordService;
import com.exam.record.vo.AuditRecordVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

/**
 * @brief 业务流程记录查询服务实现。
 *
 * @details
 * 按业务类型、业务 ID、申请 ID 或考籍档案 ID 组合查询 audit_record 表，为前端详情页、
 * 档案时间线和后续毕业申请模块提供统一流程轨迹来源。
 */
@Service
public class WorkflowRecordServiceImpl implements WorkflowRecordService {
    private static final List<String> SUPPORTED_BUSINESS_TYPES = List.of(
            "EXEMPTION", "COURSE_REPLACE", "TRANSFER_IN", "TRANSFER_OUT", "GRADUATION");

    private final AuditRecordMapper auditRecordMapper;

    /**
     * @brief 构造业务流程记录查询服务。
     *
     * @param auditRecordMapper 审核记录 Mapper。
     */
    public WorkflowRecordServiceImpl(AuditRecordMapper auditRecordMapper) {
        this.auditRecordMapper = auditRecordMapper;
    }

    /**
     * @brief 查询业务流程记录。
     *
     * @details
     * 至少需要提供一个过滤条件，避免误查全表；业务类型会统一转为大写并校验是否属于
     * 当前业务流程枚举。
     *
     * @param businessType 业务类型，可为空。
     * @param businessId 业务 ID，可为空。
     * @param applicationId 通用申请 ID，可为空。
     * @param recordId 考籍档案 ID，可为空。
     * @return 流程记录列表。
     */
    @Override
    public List<AuditRecordVO> listFlowRecords(String businessType, Long businessId, Long applicationId, Long recordId) {
        if (!StringUtils.hasText(businessType) && businessId == null && applicationId == null && recordId == null) {
            throw new BusinessException(400, "流程记录查询至少需要一个过滤条件");
        }
        LambdaQueryWrapper<AuditRecord> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(businessType)) {
            wrapper.eq(AuditRecord::getBusinessType, normalizeBusinessType(businessType));
        }
        if (businessId != null) {
            wrapper.eq(AuditRecord::getBusinessId, businessId);
        }
        if (applicationId != null) {
            wrapper.eq(AuditRecord::getApplicationId, applicationId);
        }
        if (recordId != null) {
            wrapper.eq(AuditRecord::getRecordId, recordId);
        }
        wrapper.orderByAsc(AuditRecord::getOperationTime).orderByAsc(AuditRecord::getId);
        return auditRecordMapper.selectList(wrapper).stream()
                .map(AuditRecordVO::fromEntity)
                .toList();
    }

    private String normalizeBusinessType(String businessType) {
        String normalizedType = businessType.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_BUSINESS_TYPES.contains(normalizedType)) {
            throw new BusinessException(400, "业务类型只能为EXEMPTION、COURSE_REPLACE、TRANSFER_IN、TRANSFER_OUT或GRADUATION");
        }
        return normalizedType;
    }
}
