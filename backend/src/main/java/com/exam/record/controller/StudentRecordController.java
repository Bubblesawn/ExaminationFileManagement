package com.exam.record.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.record.common.Result;
import com.exam.record.dto.StudentRecordArchiveDTO;
import com.exam.record.dto.StudentRecordCreateDTO;
import com.exam.record.dto.StudentRecordStatusUpdateDTO;
import com.exam.record.dto.StudentRecordUpdateDTO;
import com.exam.record.service.RecordChangeLogService;
import com.exam.record.service.StudentRecordService;
import com.exam.record.vo.RecordChangeLogVO;
import com.exam.record.vo.StudentRecordVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @brief 考籍档案接口。
 */
@RestController
@RequestMapping("/api/records")
public class StudentRecordController {
    private final StudentRecordService studentRecordService;
    private final RecordChangeLogService recordChangeLogService;

    /**
     * @brief 构造考籍档案控制器。
     *
     * @param studentRecordService 考籍档案业务服务。
     * @param recordChangeLogService 档案变更记录业务服务。
     */
    public StudentRecordController(StudentRecordService studentRecordService,
                                   RecordChangeLogService recordChangeLogService) {
        this.studentRecordService = studentRecordService;
        this.recordChangeLogService = recordChangeLogService;
    }

    /**
     * @brief 分页查询考籍档案。
     *
     * @param pageNo 当前页码。
     * @param pageSize 每页条数。
     * @param keyword 查询关键字。
     * @param recordStatus 考籍状态。
     * @param archiveStatus 归档状态。
     * @param candidateId 考生ID。
     * @return 考籍档案分页数据。
     */
    @GetMapping("/page")
    public Result<Page<StudentRecordVO>> page(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String recordStatus,
            @RequestParam(required = false) String archiveStatus,
            @RequestParam(required = false) Long candidateId) {
        return Result.success(studentRecordService.pageRecords(
                pageNo, pageSize, keyword, recordStatus, archiveStatus, candidateId));
    }

    /**
     * @brief 查询考籍档案详情。
     *
     * @param id 档案ID。
     * @return 考籍档案详情。
     */
    @GetMapping("/{id}")
    public Result<StudentRecordVO> detail(@PathVariable Long id) {
        return Result.success(studentRecordService.getRecordDetail(id));
    }

    /**
     * @brief 创建考籍档案。
     *
     * @param dto 新增考籍档案请求对象。
     * @return 新增后的考籍档案。
     */
    @PostMapping
    public Result<StudentRecordVO> create(@Valid @RequestBody StudentRecordCreateDTO dto) {
        return Result.success(studentRecordService.createRecord(dto));
    }

    /**
     * @brief 编辑考籍档案。
     *
     * @param id 档案ID。
     * @param dto 修改考籍档案请求对象。
     * @return 修改后的考籍档案。
     */
    @PutMapping("/{id}")
    public Result<StudentRecordVO> update(@PathVariable Long id, @Valid @RequestBody StudentRecordUpdateDTO dto) {
        return Result.success(studentRecordService.updateRecord(id, dto));
    }

    /**
     * @brief 更新考籍档案状态。
     *
     * @param id 档案ID。
     * @param dto 状态更新请求对象。
     * @return 状态更新后的考籍档案。
     */
    @PutMapping("/{id}/status")
    public Result<StudentRecordVO> updateStatus(@PathVariable Long id,
                                                @Valid @RequestBody StudentRecordStatusUpdateDTO dto) {
        return Result.success(studentRecordService.updateRecordStatus(id, dto));
    }

    /**
     * @brief 将考籍档案归档。
     *
     * @param id 档案ID。
     * @param dto 归档请求对象。
     * @return 归档后的考籍档案。
     */
    @PutMapping("/{id}/archive")
    public Result<StudentRecordVO> archive(@PathVariable Long id,
                                           @RequestBody(required = false) StudentRecordArchiveDTO dto) {
        return Result.success(studentRecordService.archiveRecord(id, dto));
    }

    /**
     * @brief 分页查询档案变更记录。
     *
     * @param id 档案ID。
     * @param pageNo 当前页码。
     * @param pageSize 每页条数。
     * @param changeType 变更类型。
     * @return 档案变更记录分页数据。
     */
    @GetMapping("/{id}/change-logs/page")
    public Result<Page<RecordChangeLogVO>> pageChangeLogs(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String changeType) {
        return Result.success(recordChangeLogService.pageRecordChangeLogs(id, pageNo, pageSize, changeType));
    }
}
