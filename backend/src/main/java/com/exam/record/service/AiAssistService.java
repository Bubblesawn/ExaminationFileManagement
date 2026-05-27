package com.exam.record.service;

import com.exam.record.dto.AiChatDTO;
import com.exam.record.dto.AiImageTaskDTO;
import com.exam.record.dto.AiSpeechDTO;
import com.exam.record.dto.ApplicationMaterialAuditDTO;
import com.exam.record.vo.AlgorithmResponseVO;

/**
 * @brief 智能辅助调用服务。
 */
public interface AiAssistService {

    /**
     * @brief 调用算法服务图像分类接口。
     *
     * @param dto 图片任务请求对象。
     * @return 算法服务响应。
     */
    AlgorithmResponseVO classifyImage(AiImageTaskDTO dto);

    /**
     * @brief 调用算法服务申请材料智能核验接口。
     *
     * @param dto 申请材料智能核验请求对象。
     * @return 算法服务响应。
     */
    AlgorithmResponseVO auditApplicationMaterials(ApplicationMaterialAuditDTO dto);

    /**
     * @brief 调用算法服务目标检测接口。
     *
     * @param dto 图片任务请求对象。
     * @return 算法服务响应。
     */
    AlgorithmResponseVO detectObjects(AiImageTaskDTO dto);

    /**
     * @brief 调用算法服务图像分割接口。
     *
     * @param dto 图片任务请求对象。
     * @return 算法服务响应。
     */
    AlgorithmResponseVO segmentImage(AiImageTaskDTO dto);

    /**
     * @brief 调用算法服务智能问答接口。
     *
     * @param dto 智能问答请求对象。
     * @return 算法服务响应。
     */
    AlgorithmResponseVO chat(AiChatDTO dto);

    /**
     * @brief 调用算法服务语音识别接口。
     *
     * @param dto 语音识别请求对象。
     * @return 算法服务响应。
     */
    AlgorithmResponseVO recognizeSpeech(AiSpeechDTO dto);

    /**
     * @brief 调用算法服务语音播报接口。
     *
     * @param dto 语音播报请求对象。
     * @return 算法服务响应。
     */
    AlgorithmResponseVO synthesizeSpeech(AiChatDTO dto);
}
