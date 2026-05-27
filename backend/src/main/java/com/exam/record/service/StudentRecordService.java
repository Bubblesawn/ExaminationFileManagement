package com.exam.record.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.record.dto.StudentRecordArchiveDTO;
import com.exam.record.dto.StudentRecordCreateDTO;
import com.exam.record.dto.StudentRecordStatusUpdateDTO;
import com.exam.record.dto.StudentRecordUpdateDTO;
import com.exam.record.entity.StudentRecord;
import com.exam.record.vo.StudentRecordVO;

/**
 * @brief 考籍档案业务接口。
 */
public interface StudentRecordService extends IService<StudentRecord> {

    /**
     * @brief 分页查询考籍档案。
     *
     * @param pageNo 当前页码。
     * @param pageSize 每页条数。
     * @param keyword 查询关键字，可匹配考籍号、专业、备注和考生身份摘要。
     * @param recordStatus 考籍状态。
     * @param archiveStatus 归档状态。
     * @param candidateId 考生ID。
     * @return 考籍档案分页数据。
     */
    Page<StudentRecordVO> pageRecords(long pageNo,
                                      long pageSize,
                                      String keyword,
                                      String recordStatus,
                                      String archiveStatus,
                                      Long candidateId);

    /**
     * @brief 查询考籍档案详情。
     *
     * @param id 档案ID。
     * @return 考籍档案详情。
     */
    StudentRecordVO getRecordDetail(Long id);

    /**
     * @brief 创建考籍档案。
     *
     * @param dto 新增考籍档案请求对象。
     * @return 新增后的考籍档案。
     */
    StudentRecordVO createRecord(StudentRecordCreateDTO dto);

    /**
     * @brief 编辑考籍档案基础信息。
     *
     * @param id 档案ID。
     * @param dto 修改考籍档案请求对象。
     * @return 修改后的考籍档案。
     */
    StudentRecordVO updateRecord(Long id, StudentRecordUpdateDTO dto);

    /**
     * @brief 更新考籍档案状态。
     *
     * @param id 档案ID。
     * @param dto 状态更新请求对象。
     * @return 状态更新后的考籍档案。
     */
    StudentRecordVO updateRecordStatus(Long id, StudentRecordStatusUpdateDTO dto);

    /**
     * @brief 将考籍档案归档。
     *
     * @param id 档案ID。
     * @param dto 归档请求对象。
     * @return 归档后的考籍档案。
     */
    StudentRecordVO archiveRecord(Long id, StudentRecordArchiveDTO dto);
}
