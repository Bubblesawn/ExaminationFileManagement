package com.exam.record.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.record.dto.CandidateCreateDTO;
import com.exam.record.entity.Candidate;
import com.exam.record.mapper.CandidateMapper;
import com.exam.record.service.CandidateService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * @brief 考生基础信息业务实现。
 */
@Service
public class CandidateServiceImpl extends ServiceImpl<CandidateMapper, Candidate> implements CandidateService {

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
            wrapper.like(Candidate::getName, keyword)
                    .or()
                    .like(Candidate::getIdCard, keyword)
                    .or()
                    .like(Candidate::getAdmissionNo, keyword);
        }
        wrapper.orderByDesc(Candidate::getCreateTime);
        return page(new Page<>(pageNo, pageSize), wrapper);
    }

    /**
     * @brief 新增考生信息。
     *
     * @param dto 新增考生请求对象。
     * @return 新增后的考生实体。
     */
    @Override
    public Candidate createCandidate(CandidateCreateDTO dto) {
        Candidate candidate = new Candidate();
        candidate.setName(dto.getName());
        candidate.setGender(dto.getGender());
        candidate.setIdCard(dto.getIdCard());
        candidate.setAdmissionNo(dto.getAdmissionNo());
        candidate.setPhone(dto.getPhone());
        candidate.setStatus("NORMAL");
        save(candidate);
        return candidate;
    }
}

