package com.exam.record.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.record.common.Result;
import com.exam.record.dto.CandidateCreateDTO;
import com.exam.record.entity.Candidate;
import com.exam.record.service.CandidateService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @brief 考生基础信息接口。
 */
@RestController
@RequestMapping("/api/candidates")
public class CandidateController {
    private final CandidateService candidateService;

    /**
     * @brief 构造考生接口控制器。
     *
     * @param candidateService 考生业务服务。
     */
    public CandidateController(CandidateService candidateService) {
        this.candidateService = candidateService;
    }

    /**
     * @brief 分页查询考生信息。
     *
     * @param pageNo 当前页码。
     * @param pageSize 每页条数。
     * @param keyword 查询关键字。
     * @return 考生分页数据。
     */
    @GetMapping("/page")
    public Result<Page<Candidate>> page(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword) {
        return Result.success(candidateService.pageCandidates(pageNo, pageSize, keyword));
    }

    /**
     * @brief 新增考生信息。
     *
     * @param dto 新增考生请求对象。
     * @return 新增后的考生信息。
     */
    @PostMapping
    public Result<Candidate> create(@Valid @RequestBody CandidateCreateDTO dto) {
        return Result.success(candidateService.createCandidate(dto));
    }
}

