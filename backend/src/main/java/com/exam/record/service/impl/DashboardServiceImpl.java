package com.exam.record.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.record.entity.BusinessApplication;
import com.exam.record.entity.RecordMaterial;
import com.exam.record.entity.StudentRecord;
import com.exam.record.mapper.BusinessApplicationMapper;
import com.exam.record.mapper.RecordMaterialMapper;
import com.exam.record.mapper.StudentRecordMapper;
import com.exam.record.service.DashboardService;
import com.exam.record.vo.DashboardStatsVO;
import org.springframework.stereotype.Service;

/**
 * @brief 工作台统计服务实现。
 *
 * @details
 * 直接通过数据库计数生成首页面板数据，避免首页为统计数字拉取业务列表明细。
 */
@Service
public class DashboardServiceImpl implements DashboardService {
    private static final String AUDIT_STATUS_PENDING = "PENDING";
    private static final String BUSINESS_TYPE_EXEMPTION = "EXEMPTION";
    private static final String BUSINESS_TYPE_GRADUATION = "GRADUATION";

    private final StudentRecordMapper studentRecordMapper;
    private final RecordMaterialMapper recordMaterialMapper;
    private final BusinessApplicationMapper businessApplicationMapper;

    /**
     * @brief 构造工作台统计服务。
     *
     * @param studentRecordMapper 考籍档案 Mapper。
     * @param recordMaterialMapper 档案材料 Mapper。
     * @param businessApplicationMapper 业务申请 Mapper。
     */
    public DashboardServiceImpl(StudentRecordMapper studentRecordMapper,
                                RecordMaterialMapper recordMaterialMapper,
                                BusinessApplicationMapper businessApplicationMapper) {
        this.studentRecordMapper = studentRecordMapper;
        this.recordMaterialMapper = recordMaterialMapper;
        this.businessApplicationMapper = businessApplicationMapper;
    }

    /**
     * @brief 查询工作台面板统计数据。
     *
     * @return 工作台统计数据。
     */
    @Override
    public DashboardStatsVO getStats() {
        Long recordCount = studentRecordMapper.selectCount(new LambdaQueryWrapper<StudentRecord>()
                .isNotNull(StudentRecord::getId));
        Long pendingMaterialCount = recordMaterialMapper.selectCount(new LambdaQueryWrapper<RecordMaterial>()
                .eq(RecordMaterial::getAuditStatus, AUDIT_STATUS_PENDING));
        Long exemptionApplicationCount = businessApplicationMapper.selectCount(new LambdaQueryWrapper<BusinessApplication>()
                .eq(BusinessApplication::getBusinessType, BUSINESS_TYPE_EXEMPTION));
        Long graduationApplicationCount = businessApplicationMapper.selectCount(new LambdaQueryWrapper<BusinessApplication>()
                .eq(BusinessApplication::getBusinessType, BUSINESS_TYPE_GRADUATION));
        return new DashboardStatsVO(recordCount, pendingMaterialCount, exemptionApplicationCount, graduationApplicationCount);
    }
}
