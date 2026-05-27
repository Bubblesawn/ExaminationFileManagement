package com.exam.record.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.record.common.Result;
import com.exam.record.dto.CandidateCreateDTO;
import com.exam.record.dto.CandidateUpdateDTO;
import com.exam.record.entity.Candidate;
import com.exam.record.service.CandidateService;
import com.exam.record.vo.CandidateImportPreviewVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
     * @brief 查询考生详情。
     *
     * @param id 考生ID。
     * @return 考生详情。
     */
    @GetMapping("/{id}")
    public Result<Candidate> detail(@PathVariable Long id) {
        return Result.success(candidateService.getCandidateDetail(id));
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

    /**
     * @brief 修改考生信息。
     *
     * @param id 考生ID。
     * @param dto 修改考生请求对象。
     * @return 修改后的考生信息。
     */
    @PutMapping("/{id}")
    public Result<Candidate> update(@PathVariable Long id, @Valid @RequestBody CandidateUpdateDTO dto) {
        return Result.success(candidateService.updateCandidate(id, dto));
    }

    /**
     * @brief 删除考生信息。
     *
     * @param id 考生ID。
     * @return 删除结果。
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        candidateService.deleteCandidate(id);
        return Result.success();
    }

    /**
     * @brief 预览导入考生信息。
     *
     * @details
     * 当前接口作为第三阶段导入能力预留入口，先校验文件存在并返回固定导入字段说明，后续可扩展为 Excel 解析和批量入库。
     *
     * @param file 待导入文件。
     * @return 导入预览占位结果。
     */
    @PostMapping("/import/preview")
    public Result<CandidateImportPreviewVO> previewImport(@RequestParam("file") MultipartFile file) {
        return Result.success(candidateService.previewImport(file));
    }
}
