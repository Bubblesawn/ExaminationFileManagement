package com.exam.record.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.record.common.BusinessException;
import com.exam.record.dto.CandidateCreateDTO;
import com.exam.record.dto.CandidateUpdateDTO;
import com.exam.record.entity.Candidate;
import com.exam.record.mapper.CandidateMapper;
import com.exam.record.service.CandidateService;
import com.exam.record.vo.CandidateImportPreviewVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @brief 考生基础信息业务实现。
 */
@Service
public class CandidateServiceImpl extends ServiceImpl<CandidateMapper, Candidate> implements CandidateService {
    private static final String STATUS_NORMAL = "NORMAL";
    private static final String STATUS_LOCKED = "LOCKED";
    private static final String STATUS_DISABLED = "DISABLED";
    private static final List<String> IMPORT_EXPECTED_HEADERS = List.of(
            "姓名", "性别", "身份证号", "准考证号", "出生日期", "民族", "政治面貌",
            "学历层次", "报考专业", "联系电话", "电子邮箱", "联系地址"
    );


    /**
     * @brief 分页查询考生信息。
     *
     * @param pageNo 当前页码。
     * @param pageSize 每页条数。
     * @param keyword 查询关键字，可匹配姓名、身份证号或准考证号。
     * @return 考生分页数据。
     */
    @Override
    public Page<Candidate> pageCandidates(long pageNo, long pageSize, String keyword) {
        LambdaQueryWrapper<Candidate> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query.like(Candidate::getName, keyword)
                    .or()
                    .like(Candidate::getIdCard, keyword)
                    .or()
                    .like(Candidate::getAdmissionNo, keyword));
        }
        wrapper.orderByDesc(Candidate::getCreateTime);
        return page(new Page<>(pageNo, pageSize), wrapper);
    }

    /**
     * @brief 查询考生详情。
     *
     * @param id 考生ID。
     * @return 考生详情。
     */
    @Override
    public Candidate getCandidateDetail(Long id) {
        return getExistingCandidate(id);
    }

    /**
     * @brief 新增考生信息。
     *
     * @param dto 新增考生请求对象。
     * @return 新增后的考生实体。
     */
    @Override
    public Candidate createCandidate(CandidateCreateDTO dto) {
        validateUniqueIdCard(dto.getIdCard(), null);
        validateUniqueAdmissionNo(dto.getAdmissionNo(), null);
        Candidate candidate = new Candidate();
        fillCandidate(candidate, dto.getName(), dto.getGender(), dto.getIdCard(), dto.getAdmissionNo(),
                dto.getBirthDate(), dto.getNation(), dto.getPoliticalStatus(), dto.getEducationLevel(),
                dto.getMajorName(), dto.getPhone(), dto.getEmail(), dto.getAddress(), dto.getStatus());
        save(candidate);
        return candidate;
    }

    /**
     * @brief 修改考生信息。
     *
     * @param id 考生ID。
     * @param dto 修改考生请求对象。
     * @return 修改后的考生实体。
     */
    @Override
    public Candidate updateCandidate(Long id, CandidateUpdateDTO dto) {
        Candidate candidate = getExistingCandidate(id);
        validateUniqueIdCard(dto.getIdCard(), id);
        validateUniqueAdmissionNo(dto.getAdmissionNo(), id);
        fillCandidate(candidate, dto.getName(), dto.getGender(), dto.getIdCard(), dto.getAdmissionNo(),
                dto.getBirthDate(), dto.getNation(), dto.getPoliticalStatus(), dto.getEducationLevel(),
                dto.getMajorName(), dto.getPhone(), dto.getEmail(), dto.getAddress(), dto.getStatus());
        updateById(candidate);
        return getById(id);
    }

    /**
     * @brief 删除考生信息。
     *
     * @details
     * 删除前会校验考生存在性。当前阶段尚未接入考籍档案业务关联校验，后续在档案接口完成后补充有关联档案时禁止删除的规则。
     *
     * @param id 考生ID。
     */
    @Override
    public void deleteCandidate(Long id) {
        getExistingCandidate(id);
        removeById(id);
    }

    /**
     * @brief 预留考生导入接口。
     *
     * @details
     * 当前版本仅校验上传文件并返回导入模板字段，作为前端联调和后续 Excel 解析功能的稳定入口。
     *
     * @param file 待导入的考生信息文件。
     * @return 导入预览占位结果。
     */
    @Override
    public CandidateImportPreviewVO previewImport(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "导入文件不能为空");
        }
        CandidateImportPreviewVO preview = new CandidateImportPreviewVO();
        preview.setFileName(file.getOriginalFilename());
        preview.setFileSize(file.getSize());
        preview.setExpectedHeaders(IMPORT_EXPECTED_HEADERS);
        preview.setTotalRows(0);
        preview.setValidRows(0);
        preview.setInvalidRows(0);
        preview.setMessage("导入解析功能已预留，当前版本暂不写入考生数据");
        return preview;
    }

    private Candidate getExistingCandidate(Long id) {
        Candidate candidate = getById(id);
        if (candidate == null) {
            throw new BusinessException(404, "考生不存在");
        }
        return candidate;
    }

    private void fillCandidate(
            Candidate candidate,
            String name,
            String gender,
            String idCard,
            String admissionNo,
            java.time.LocalDate birthDate,
            String nation,
            String politicalStatus,
            String educationLevel,
            String majorName,
            String phone,
            String email,
            String address,
            String status) {
        candidate.setName(name);
        candidate.setGender(gender);
        candidate.setIdCard(idCard);
        candidate.setAdmissionNo(admissionNo);
        candidate.setBirthDate(birthDate);
        candidate.setNation(nation);
        candidate.setPoliticalStatus(politicalStatus);
        candidate.setEducationLevel(educationLevel);
        candidate.setMajorName(majorName);
        candidate.setPhone(phone);
        candidate.setEmail(email);
        candidate.setAddress(address);
        candidate.setStatus(StringUtils.hasText(status) ? status : STATUS_NORMAL);
        validateStatus(candidate.getStatus());
    }

    private void validateUniqueIdCard(String idCard, Long excludeId) {
        LambdaQueryWrapper<Candidate> wrapper = new LambdaQueryWrapper<Candidate>()
                .eq(Candidate::getIdCard, idCard);
        if (excludeId != null) {
            wrapper.ne(Candidate::getId, excludeId);
        }
        if (count(wrapper) > 0) {
            throw new BusinessException(409, "身份证号已存在");
        }
    }

    private void validateUniqueAdmissionNo(String admissionNo, Long excludeId) {
        if (!StringUtils.hasText(admissionNo)) {
            return;
        }
        LambdaQueryWrapper<Candidate> wrapper = new LambdaQueryWrapper<Candidate>()
                .eq(Candidate::getAdmissionNo, admissionNo);
        if (excludeId != null) {
            wrapper.ne(Candidate::getId, excludeId);
        }
        if (count(wrapper) > 0) {
            throw new BusinessException(409, "准考证号已存在");
        }
    }

    /**
     * @brief 校验考生状态值是否合法。
     *
     * @details
     * 考生状态与数据库设计、测试数据和前端状态选项保持一致，允许正常、锁定和停用三种状态。
     *
     * @param status 待校验的考生状态编码。
     */
    private void validateStatus(String status) {
        if (!STATUS_NORMAL.equals(status) && !STATUS_LOCKED.equals(status) && !STATUS_DISABLED.equals(status)) {
            throw new BusinessException(400, "考生状态只能为NORMAL、LOCKED或DISABLED");
        }
    }
}
