package com.exam.record.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.record.entity.BusinessApplication;
import com.exam.record.entity.RecordMaterial;
import com.exam.record.entity.StudentRecord;
import com.exam.record.mapper.BusinessApplicationMapper;
import com.exam.record.mapper.RecordMaterialMapper;
import com.exam.record.mapper.StudentRecordMapper;
import com.exam.record.service.DashboardService;
import com.exam.record.service.UserPermissionService;
import com.exam.record.util.AuthContextHolder;
import com.exam.record.vo.DashboardStatsVO;
import com.exam.record.vo.TokenUserVO;
import org.springframework.stereotype.Service;

import java.util.Set;

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
    private static final String PERMISSION_RECORD_VIEW = "record:view";
    private static final String PERMISSION_MATERIAL_AUDIT_VIEW = "material:audit:view";
    private static final String PERMISSION_EXEMPTION_VIEW = "exemption:view";
    private static final String PERMISSION_GRADUATION_VIEW = "graduation:view";

    private final StudentRecordMapper studentRecordMapper;
    private final RecordMaterialMapper recordMaterialMapper;
    private final BusinessApplicationMapper businessApplicationMapper;
    private final UserPermissionService userPermissionService;

    /**
     * @brief 构造工作台统计服务。
     *
     * @param studentRecordMapper 考籍档案 Mapper。
     * @param recordMaterialMapper 档案材料 Mapper。
     * @param businessApplicationMapper 业务申请 Mapper。
     * @param userPermissionService 用户权限查询服务。
     */
    public DashboardServiceImpl(StudentRecordMapper studentRecordMapper,
                                RecordMaterialMapper recordMaterialMapper,
                                BusinessApplicationMapper businessApplicationMapper,
                                UserPermissionService userPermissionService) {
        this.studentRecordMapper = studentRecordMapper;
        this.recordMaterialMapper = recordMaterialMapper;
        this.businessApplicationMapper = businessApplicationMapper;
        this.userPermissionService = userPermissionService;
    }

    /**
     * @brief 查询工作台面板统计数据。
     *
     * @details
     * 工作台入口面向所有已授权用户开放，但统计卡片只返回当前用户已拥有菜单权限范围内的数据，
     * 避免普通角色通过首页聚合接口看到无权限业务模块的数量。
     *
     * @return 工作台统计数据。
     */
    @Override
    public DashboardStatsVO getStats() {
        Set<String> permissions = currentPermissions();
        Long recordCount = permissions.contains(PERMISSION_RECORD_VIEW)
                ? studentRecordMapper.selectCount(new LambdaQueryWrapper<StudentRecord>().isNotNull(StudentRecord::getId))
                : 0L;
        Long pendingMaterialCount = permissions.contains(PERMISSION_MATERIAL_AUDIT_VIEW)
                ? recordMaterialMapper.selectCount(new LambdaQueryWrapper<RecordMaterial>()
                        .eq(RecordMaterial::getAuditStatus, AUDIT_STATUS_PENDING))
                : 0L;
        Long exemptionApplicationCount = permissions.contains(PERMISSION_EXEMPTION_VIEW)
                ? businessApplicationMapper.selectCount(new LambdaQueryWrapper<BusinessApplication>()
                        .eq(BusinessApplication::getBusinessType, BUSINESS_TYPE_EXEMPTION))
                : 0L;
        Long graduationApplicationCount = permissions.contains(PERMISSION_GRADUATION_VIEW)
                ? businessApplicationMapper.selectCount(new LambdaQueryWrapper<BusinessApplication>()
                        .eq(BusinessApplication::getBusinessType, BUSINESS_TYPE_GRADUATION))
                : 0L;
        return new DashboardStatsVO(recordCount, pendingMaterialCount, exemptionApplicationCount, graduationApplicationCount);
    }

    private Set<String> currentPermissions() {
        TokenUserVO currentUser = AuthContextHolder.getUser();
        if (currentUser == null || currentUser.getId() == null) {
            return Set.of();
        }
        return userPermissionService.listPermissions(currentUser.getId());
    }
}
