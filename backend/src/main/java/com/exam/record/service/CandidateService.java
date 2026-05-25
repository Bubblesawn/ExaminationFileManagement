package com.exam.record.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.record.dto.CandidateCreateDTO;
import com.exam.record.entity.Candidate;

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
     * @brief 新增考生信息。
     *
     * @param dto 新增考生请求对象。
     * @return 新增后的考生实体。
     */
    Candidate createCandidate(CandidateCreateDTO dto);
}

