package com.exam.record.service.impl;

import com.exam.record.dto.AiChatDTO;
import com.exam.record.dto.AiImageTaskDTO;
import com.exam.record.dto.AiSpeechDTO;
import com.exam.record.dto.ApplicationMaterialAuditDTO;
import com.exam.record.service.AiAssistService;
import com.exam.record.vo.AlgorithmResponseVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * @brief 智能辅助调用服务实现。
 *
 * @details
 * 统一封装后端到 FastAPI 算法服务的调用入口，并记录请求结果和异常日志，
 * 方便后续接入数据库日志表或链路追踪组件。
 */
@Slf4j
@Service
public class AiAssistServiceImpl implements AiAssistService {
    private final RestTemplate restTemplate;
    private final String algorithmBaseUrl;

    /**
     * @brief 构造智能辅助调用服务。
     *
     * @param restTemplate HTTP 客户端。
     * @param algorithmBaseUrl 算法服务基础地址。
     */
    public AiAssistServiceImpl(
            RestTemplate restTemplate,
            @Value("${algorithm.service.base-url}") String algorithmBaseUrl) {
        this.restTemplate = restTemplate;
        this.algorithmBaseUrl = trimTrailingSlash(algorithmBaseUrl);
    }

    /**
     * @brief 调用算法服务图像分类接口。
     *
     * @param dto 图片任务请求对象。
     * @return 算法服务响应。
     */
    @Override
    public AlgorithmResponseVO classifyImage(AiImageTaskDTO dto) {
        return callAlgorithm("/image-classify", dto, "图像分类", dto.getBusinessId(), dto.getScene());
    }

    /**
     * @brief 调用算法服务申请材料智能核验接口。
     *
     * @param dto 申请材料智能核验请求对象。
     * @return 算法服务响应。
     */
    @Override
    public AlgorithmResponseVO auditApplicationMaterials(ApplicationMaterialAuditDTO dto) {
        return callAlgorithm("/application-material-audit", dto, "申请材料核验", dto.getBusinessId(), dto.getApplicationType());
    }

    /**
     * @brief 调用算法服务目标检测接口。
     *
     * @param dto 图片任务请求对象。
     * @return 算法服务响应。
     */
    @Override
    public AlgorithmResponseVO detectObjects(AiImageTaskDTO dto) {
        return callAlgorithm("/object-detect", dto, "目标检测", dto.getBusinessId(), dto.getScene());
    }

    /**
     * @brief 调用算法服务图像分割接口。
     *
     * @param dto 图片任务请求对象。
     * @return 算法服务响应。
     */
    @Override
    public AlgorithmResponseVO segmentImage(AiImageTaskDTO dto) {
        return callAlgorithm("/image-segment", dto, "图像分割", dto.getBusinessId(), dto.getScene());
    }

    /**
     * @brief 调用算法服务智能问答接口。
     *
     * @param dto 智能问答请求对象。
     * @return 算法服务响应。
     */
    @Override
    public AlgorithmResponseVO chat(AiChatDTO dto) {
        return callAlgorithm("/chat", dto, "智能问答", dto.getBusinessId(), dto.getScene());
    }

    /**
     * @brief 调用算法服务语音识别接口。
     *
     * @param dto 语音识别请求对象。
     * @return 算法服务响应。
     */
    @Override
    public AlgorithmResponseVO recognizeSpeech(AiSpeechDTO dto) {
        return callAlgorithm("/asr", dto, "语音识别", dto.getBusinessId(), dto.getScene());
    }

    /**
     * @brief 调用算法服务语音播报接口。
     *
     * @param dto 语音播报请求对象。
     * @return 算法服务响应。
     */
    @Override
    public AlgorithmResponseVO synthesizeSpeech(AiChatDTO dto) {
        return callAlgorithm("/tts", dto, "语音播报", dto.getBusinessId(), dto.getScene());
    }

    /**
     * @brief 统一调用算法服务并记录调用日志。
     *
     * @param path 算法服务接口路径。
     * @param request 请求对象。
     * @param actionName 智能辅助动作名称。
     * @param businessId 关联业务 ID。
     * @param scene 业务场景。
     * @return 算法服务响应。
     */
    private AlgorithmResponseVO callAlgorithm(
            String path,
            Object request,
            String actionName,
            Long businessId,
            String scene) {
        String url = algorithmBaseUrl + path;
        long startTime = System.currentTimeMillis();
        try {
            AlgorithmResponseVO response = restTemplate.postForObject(url, request, AlgorithmResponseVO.class);
            long cost = System.currentTimeMillis() - startTime;
            if (response == null) {
                log.error("智能辅助调用失败 action={} businessId={} scene={} url={} cost={}ms reason=响应为空",
                        actionName, businessId, scene, url, cost);
                throw new IllegalStateException(actionName + "算法服务响应为空");
            }
            if (Integer.valueOf(200).equals(response.getCode())) {
                log.info("智能辅助调用成功 action={} businessId={} scene={} url={} cost={}ms",
                        actionName, businessId, scene, url, cost);
            } else {
                log.warn("智能辅助调用返回业务失败 action={} businessId={} scene={} url={} cost={}ms code={} message={}",
                        actionName, businessId, scene, url, cost, response.getCode(), response.getMessage());
            }
            return response;
        } catch (RestClientException exception) {
            long cost = System.currentTimeMillis() - startTime;
            log.error("智能辅助调用异常 action={} businessId={} scene={} url={} cost={}ms",
                    actionName, businessId, scene, url, cost, exception);
            throw new IllegalStateException(actionName + "算法服务调用失败：" + exception.getMessage(), exception);
        }
    }

    /**
     * @brief 去除服务地址末尾斜杠。
     *
     * @param value 服务地址。
     * @return 规范化后的服务地址。
     */
    private String trimTrailingSlash(String value) {
        if (value != null && value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}
