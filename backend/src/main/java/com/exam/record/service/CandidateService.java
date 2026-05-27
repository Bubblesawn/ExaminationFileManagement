package com.exam.record.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.record.dto.CandidateCreateDTO;
import com.exam.record.dto.CandidateUpdateDTO;
import com.exam.record.entity.Candidate;
import com.exam.record.vo.CandidateImportPreviewVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * @brief 考生基础信息业务接口。
 */
public interface CandidateService extends IService<Candidate> {

    /**
     * @brief 分页查询考生信息。
     *
     * @param pageNo 当前页码。
     * @param pageSize 每页条数。
     * @param keyword 查询关键字。
     * @return 考生分页数据。
     */
    Page<Candidate> pageCandidates(long pageNo, long pageSize, String keyword);

    /**
     * @brief 查询考生详情。
     *
     * @param id 考生ID。
     * @return 考生详情。
     */
    Candidate getCandidateDetail(Long id);

    /**
     * @brief 新增考生信息。
     *
     * @param dto 新增考生请求对象。
     * @return 新增后的考生实体。
     */
    Candidate createCandidate(CandidateCreateDTO dto);

    /**
     * @brief 修改考生信息。
     *
     * @param id 考生ID。
     * @param dto 修改考生请求对象。
     * @return 修改后的考生实体。
     */
    Candidate updateCandidate(Long id, CandidateUpdateDTO dto);

    /**
     * @brief 删除考生信息。
     *
     * @param id 考生ID。
     */
    void deleteCandidate(Long id);

    /**
     * @brief 预留考生导入接口。
     *
     * @param file 待导入的考生信息文件。
     * @return 导入预览占位结果。
     */
    CandidateImportPreviewVO previewImport(MultipartFile file);
}
