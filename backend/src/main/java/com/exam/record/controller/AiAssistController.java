package com.exam.record.controller;

import com.exam.record.common.Result;
import com.exam.record.dto.AiChatDTO;
import com.exam.record.dto.AiImageTaskDTO;
import com.exam.record.dto.AiSpeechDTO;
import com.exam.record.dto.ApplicationMaterialAuditDTO;
import com.exam.record.dto.MaterialPreprocessDTO;
import com.exam.record.service.AiAssistService;
import com.exam.record.vo.AlgorithmResponseVO;
import com.exam.record.vo.MaterialUploadVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @brief 智能辅助后端封装接口。
 *
 * @details
 * 为前端和业务模块提供统一的智能辅助调用入口，屏蔽算法服务地址和
 * 调用细节，并由服务层统一记录请求结果和异常日志。
 */
@RestController
@RequestMapping("/api/ai")
public class AiAssistController {
    private final AiAssistService aiAssistService;

    /**
     * @brief 构造智能辅助控制器。
     *
     * @param aiAssistService 智能辅助调用服务。
     */
    public AiAssistController(AiAssistService aiAssistService) {
        this.aiAssistService = aiAssistService;
    }

    /**
     * @brief 调用图像分类能力。
     *
     * @param dto 图片任务请求对象。
     * @return 图像分类算法响应。
     */
    @PostMapping("/image-classify")
    public Result<AlgorithmResponseVO> classifyImage(@Valid @RequestBody AiImageTaskDTO dto) {
        return Result.success(aiAssistService.classifyImage(dto));
    }

    /**
     * @brief 上传真实材料文件。
     *
     * @param file 前端上传的材料图片文件。
     * @return 材料文件访问地址和元信息。
     */
    @PostMapping("/materials/upload")
    public Result<MaterialUploadVO> uploadMaterial(MultipartFile file) {
        return Result.success(aiAssistService.uploadMaterial(file));
    }

    /**
     * @brief 调用材料预处理能力。
     *
     * @param dto 材料预处理请求对象。
     * @return 材料格式校验、图片清晰度检测和基础分类算法响应。
     */
    @PostMapping("/material-preprocess")
    public Result<AlgorithmResponseVO> preprocessMaterial(@Valid @RequestBody MaterialPreprocessDTO dto) {
        return Result.success(aiAssistService.preprocessMaterial(dto));
    }

    /**
     * @brief 调用申请材料智能核验能力。
     *
     * @param dto 申请材料智能核验请求对象。
     * @return 申请材料分类、缺失项和异常提醒算法响应。
     */
    @PostMapping("/application-material-audit")
    public Result<AlgorithmResponseVO> auditApplicationMaterials(@Valid @RequestBody ApplicationMaterialAuditDTO dto) {
        return Result.success(aiAssistService.auditApplicationMaterials(dto));
    }

    /**
     * @brief 按业务申请 ID 调用申请材料智能核验能力。
     *
     * @details
     * 后端根据 business_application.material_ids_json 自动加载已绑定材料，
     * 组装材料访问地址、文件名和登记类别后再调用算法服务，避免前端重复拼装材料明细。
     *
     * @param applicationId 业务申请 ID。
     * @return 申请材料分类、缺失项和异常提醒算法响应。
     */
    @PostMapping("/applications/{applicationId}/material-audit")
    public Result<AlgorithmResponseVO> auditApplicationMaterialsByApplicationId(@PathVariable Long applicationId) {
        return Result.success(aiAssistService.auditApplicationMaterialsByApplicationId(applicationId));
    }

    /**
     * @brief 调用目标检测能力。
     *
     * @param dto 图片任务请求对象。
     * @return 目标检测算法响应。
     */
    @PostMapping("/object-detect")
    public Result<AlgorithmResponseVO> detectObjects(@Valid @RequestBody AiImageTaskDTO dto) {
        return Result.success(aiAssistService.detectObjects(dto));
    }

    /**
     * @brief 调用图像分割能力。
     *
     * @param dto 图片任务请求对象。
     * @return 图像分割算法响应。
     */
    @PostMapping("/image-segment")
    public Result<AlgorithmResponseVO> segmentImage(@Valid @RequestBody AiImageTaskDTO dto) {
        return Result.success(aiAssistService.segmentImage(dto));
    }

    /**
     * @brief 调用智能问答能力。
     *
     * @param dto 智能问答请求对象。
     * @return 智能问答算法响应。
     */
    @PostMapping("/chat")
    public Result<AlgorithmResponseVO> chat(@Valid @RequestBody AiChatDTO dto) {
        return Result.success(aiAssistService.chat(dto));
    }

    /**
     * @brief 调用语音识别能力。
     *
     * @param dto 语音识别请求对象。
     * @return 语音识别算法响应。
     */
    @PostMapping("/asr")
    public Result<AlgorithmResponseVO> recognizeSpeech(@Valid @RequestBody AiSpeechDTO dto) {
        return Result.success(aiAssistService.recognizeSpeech(dto));
    }

    /**
     * @brief 调用语音播报能力。
     *
     * @param dto 语音播报请求对象。
     * @return 语音播报算法响应。
     */
    @PostMapping("/tts")
    public Result<AlgorithmResponseVO> synthesizeSpeech(@Valid @RequestBody AiChatDTO dto) {
        return Result.success(aiAssistService.synthesizeSpeech(dto));
    }
}
