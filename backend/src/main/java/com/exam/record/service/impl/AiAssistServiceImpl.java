package com.exam.record.service.impl;

import com.exam.record.common.BusinessException;
import com.exam.record.dto.AiChatDTO;
import com.exam.record.dto.AiImageTaskDTO;
import com.exam.record.dto.AiSpeechDTO;
import com.exam.record.dto.ApplicationMaterialAuditDTO;
import com.exam.record.dto.MaterialPreprocessDTO;
import com.exam.record.service.AiAssistService;
import com.exam.record.vo.AlgorithmResponseVO;
import com.exam.record.vo.MaterialUploadVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

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
    private static final String ALGORITHM_API_KEY_HEADER = "X-Internal-Api-Key";
    private static final long MAX_MATERIAL_FILE_SIZE = 10 * 1024 * 1024;
    private static final Set<String> SUPPORTED_MATERIAL_SUFFIXES = Set.of("jpg", "jpeg", "png", "bmp", "webp");
    private static final DateTimeFormatter UPLOAD_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RestTemplate restTemplate;
    private final String algorithmBaseUrl;
    private final String algorithmApiKey;
    private final Path uploadRootPath;

    /**
     * @brief 构造智能辅助调用服务。
     *
     * @param restTemplate HTTP 客户端。
     * @param algorithmBaseUrl 算法服务基础地址。
     * @param algorithmApiKey 后端访问算法服务的内部 API Key。
     */
    public AiAssistServiceImpl(
            RestTemplate restTemplate,
            @Value("${algorithm.service.base-url}") String algorithmBaseUrl,
            @Value("${algorithm.service.api-key}") String algorithmApiKey,
            @Value("${material.upload.root:uploads/materials}") String uploadRoot) {
        this.restTemplate = restTemplate;
        this.algorithmBaseUrl = trimTrailingSlash(algorithmBaseUrl);
        this.algorithmApiKey = algorithmApiKey;
        this.uploadRootPath = Path.of(uploadRoot).toAbsolutePath().normalize();
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
     * @brief 保存上传的真实材料图片。
     *
     * @details
     * 先校验文件大小和图片后缀，再按日期目录写入本地上传目录，
     * 最后返回可由前端和算法联调继续使用的静态访问地址。
     *
     * @param file 前端上传的材料图片文件。
     * @return 上传后的材料文件元信息。
     */
    @Override
    public MaterialUploadVO uploadMaterial(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请先选择要上传的材料文件");
        }
        if (file.getSize() > MAX_MATERIAL_FILE_SIZE) {
            throw new BusinessException(400, "材料文件不能超过 10MB");
        }

        String originalFilename = file.getOriginalFilename();
        String suffix = getFileSuffix(originalFilename);
        if (!SUPPORTED_MATERIAL_SUFFIXES.contains(suffix)) {
            throw new BusinessException(415, "仅支持 jpg、jpeg、png、bmp、webp 格式材料");
        }

        String dateDirectoryName = LocalDate.now().format(UPLOAD_DATE_FORMATTER);
        String savedFilename = UUID.randomUUID() + "." + suffix;
        Path targetDirectory = uploadRootPath.resolve(dateDirectoryName).normalize();
        Path targetPath = targetDirectory.resolve(savedFilename).normalize();
        if (!targetPath.startsWith(uploadRootPath)) {
            throw new BusinessException(400, "材料文件路径不合法");
        }

        try {
            Files.createDirectories(targetDirectory);
            file.transferTo(targetPath);
        } catch (IOException exception) {
            log.error("材料文件上传失败 originalFilename={} targetPath={}", originalFilename, targetPath, exception);
            throw new BusinessException(500, "材料文件保存失败");
        }

        return new MaterialUploadVO(
                originalFilename,
                "/uploads/materials/" + dateDirectoryName + "/" + savedFilename,
                file.getContentType(),
                file.getSize());
    }

    /**
     * @brief 调用算法服务材料预处理接口。
     *
     * @param dto 材料预处理请求对象。
     * @return 算法服务响应。
     */
    @Override
    public AlgorithmResponseVO preprocessMaterial(MaterialPreprocessDTO dto) {
        return callAlgorithm("/material-preprocess", dto, "材料预处理", dto.getBusinessId(), dto.getScene());
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
            AlgorithmResponseVO response = restTemplate.postForObject(
                    url,
                    new HttpEntity<>(request, buildAlgorithmHeaders()),
                    AlgorithmResponseVO.class);
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
     * @brief 构造后端访问算法服务的内部认证请求头。
     *
     * @return 包含内部 API Key 的请求头。
     */
    private HttpHeaders buildAlgorithmHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(ALGORITHM_API_KEY_HEADER, algorithmApiKey);
        return headers;
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

    /**
     * @brief 提取上传文件扩展名。
     *
     * @param filename 上传文件原始名称。
     * @return 小写文件扩展名，不包含点号。
     */
    private String getFileSuffix(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
