package com.exam.record.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.record.common.BusinessException;
import com.exam.record.dto.RecordMaterialUploadDTO;
import com.exam.record.entity.MaterialType;
import com.exam.record.entity.RecordMaterial;
import com.exam.record.entity.StudentRecord;
import com.exam.record.mapper.MaterialTypeMapper;
import com.exam.record.mapper.RecordMaterialMapper;
import com.exam.record.mapper.StudentRecordMapper;
import com.exam.record.service.RecordMaterialService;
import com.exam.record.util.AuthContextHolder;
import com.exam.record.vo.MaterialFileResourceVO;
import com.exam.record.vo.RecordMaterialVO;
import com.exam.record.vo.TokenUserVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * @brief 档案材料业务实现。
 *
 * @details
 * 负责材料文件本地存储、材料记录入库、下载预览资源读取和单条材料删除。
 * 文件保存路径按日期分目录，数据库仅保存相对访问地址，便于后续替换为对象存储。
 */
@Service
public class RecordMaterialServiceImpl extends ServiceImpl<RecordMaterialMapper, RecordMaterial>
        implements RecordMaterialService {
    private static final String AUDIT_STATUS_PENDING = "PENDING";
    private static final Set<String> ALLOWED_SUFFIXES = Set.of("jpg", "jpeg", "png", "pdf");

    private final StudentRecordMapper studentRecordMapper;
    private final MaterialTypeMapper materialTypeMapper;
    private final Path uploadRootPath;

    /**
     * @brief 构造档案材料业务实现。
     *
     * @param studentRecordMapper 考籍档案 Mapper。
     * @param materialTypeMapper 材料类型 Mapper。
     * @param uploadRoot 文件上传根目录。
     */
    public RecordMaterialServiceImpl(StudentRecordMapper studentRecordMapper,
                                     MaterialTypeMapper materialTypeMapper,
                                     @Value("${file.storage.material-root:uploads/materials}") String uploadRoot) {
        this.studentRecordMapper = studentRecordMapper;
        this.materialTypeMapper = materialTypeMapper;
        this.uploadRootPath = Paths.get(uploadRoot).toAbsolutePath().normalize();
    }

    /**
     * @brief 查询指定考籍档案下的材料列表。
     *
     * @param recordId 考籍档案ID。
     * @param materialType 材料类型，可为空。
     * @return 档案材料列表。
     */
    @Override
    public List<RecordMaterialVO> listMaterials(Long recordId, String materialType) {
        LambdaQueryWrapper<RecordMaterial> wrapper = new LambdaQueryWrapper<>();
        if (recordId != null) {
            wrapper.eq(RecordMaterial::getRecordId, recordId);
        }
        if (StringUtils.hasText(materialType)) {
            wrapper.eq(RecordMaterial::getMaterialType, materialType);
        }
        wrapper.orderByDesc(RecordMaterial::getCreateTime);
        return list(wrapper).stream().map(RecordMaterialVO::fromEntity).toList();
    }

    /**
     * @brief 上传档案材料。
     *
     * @details
     * 上传前校验档案存在、材料类型启用、文件非空和文件格式，随后将文件复制到
     * 本地材料目录并写入 record_material 记录。
     *
     * @param dto 材料上传业务字段。
     * @param file 上传文件。
     * @return 上传后的材料记录。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RecordMaterialVO uploadMaterial(RecordMaterialUploadDTO dto, MultipartFile file) {
        validateRecordExists(dto.getRecordId());
        validateEnabledMaterialType(dto.getMaterialType());
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "上传材料文件不能为空");
        }
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "material" : file.getOriginalFilename());
        String suffix = extractSuffix(originalFileName);
        validateSuffix(suffix);
        String storedFileName = UUID.randomUUID() + "." + suffix;
        Path relativePath = Paths.get(String.valueOf(dto.getRecordId()), LocalDate.now().toString(), storedFileName);
        Path targetPath = uploadRootPath.resolve(relativePath).normalize();
        if (!targetPath.startsWith(uploadRootPath)) {
            throw new BusinessException(400, "文件保存路径非法");
        }
        try {
            Files.createDirectories(targetPath.getParent());
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new BusinessException(500, "材料文件保存失败：" + exception.getMessage());
        }

        TokenUserVO user = AuthContextHolder.getUser();
        RecordMaterial material = new RecordMaterial();
        material.setRecordId(dto.getRecordId());
        material.setMaterialType(dto.getMaterialType());
        material.setFileName(storedFileName);
        material.setOriginalFileName(originalFileName);
        material.setFileUrl("/api/materials/" + storedFileName + "/download");
        material.setFileSize(file.getSize());
        material.setFileSuffix(suffix);
        material.setMimeType(file.getContentType());
        material.setPreviewUrl("/api/materials/" + storedFileName + "/preview");
        material.setUploadUserId(user == null ? null : user.getId());
        material.setAuditStatus(AUDIT_STATUS_PENDING);
        save(material);
        material.setFileUrl("/api/materials/" + material.getId() + "/download");
        material.setPreviewUrl("/api/materials/" + material.getId() + "/preview");
        updateById(material);
        return RecordMaterialVO.fromEntity(getById(material.getId()));
    }

    /**
     * @brief 查询材料下载资源。
     *
     * @param id 材料ID。
     * @return 文件资源响应对象。
     */
    @Override
    public MaterialFileResourceVO loadDownloadResource(Long id) {
        return loadFileResource(getExistingMaterial(id));
    }

    /**
     * @brief 查询材料预览资源。
     *
     * @param id 材料ID。
     * @return 文件资源响应对象。
     */
    @Override
    public MaterialFileResourceVO loadPreviewResource(Long id) {
        return loadFileResource(getExistingMaterial(id));
    }

    /**
     * @brief 删除单条档案材料及其本地文件。
     *
     * @details
     * 删除动作只处理当前材料记录和该记录对应的单个物理文件；文件不存在时仍删除
     * 数据库记录，避免历史坏链路阻断业务清理。
     *
     * @param id 材料ID。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMaterial(Long id) {
        RecordMaterial material = getExistingMaterial(id);
        Path filePath = resolveMaterialPath(material);
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException exception) {
            throw new BusinessException(500, "材料文件删除失败：" + exception.getMessage());
        }
        removeById(id);
    }

    private void validateRecordExists(Long recordId) {
        StudentRecord record = studentRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(404, "考籍档案不存在");
        }
    }

    private void validateEnabledMaterialType(String materialType) {
        Long count = materialTypeMapper.selectCount(new LambdaQueryWrapper<MaterialType>()
                .eq(MaterialType::getTypeCode, materialType)
                .eq(MaterialType::getStatus, "ENABLED"));
        if (count == null || count == 0) {
            throw new BusinessException(400, "材料类型不存在或未启用");
        }
    }

    private RecordMaterial getExistingMaterial(Long id) {
        RecordMaterial material = getById(id);
        if (material == null) {
            throw new BusinessException(404, "材料不存在");
        }
        return material;
    }

    private String extractSuffix(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            throw new BusinessException(400, "材料文件必须包含扩展名");
        }
        return fileName.substring(index + 1).toLowerCase();
    }

    private void validateSuffix(String suffix) {
        if (!ALLOWED_SUFFIXES.contains(suffix)) {
            throw new BusinessException(415, "材料文件仅支持jpg、jpeg、png和pdf格式");
        }
    }

    private MaterialFileResourceVO loadFileResource(RecordMaterial material) {
        Path filePath = resolveMaterialPath(material);
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BusinessException(404, "材料文件不存在或不可读");
            }
            return new MaterialFileResourceVO(resource, material.getOriginalFileName(), material.getMimeType());
        } catch (MalformedURLException exception) {
            throw new BusinessException(500, "材料文件路径非法");
        }
    }

    private Path resolveMaterialPath(RecordMaterial material) {
        Path candidate = uploadRootPath.resolve(String.valueOf(material.getRecordId()))
                .resolve(material.getCreateTime().toLocalDate().toString())
                .resolve(material.getFileName())
                .normalize();
        if (!candidate.startsWith(uploadRootPath)) {
            throw new BusinessException(400, "材料文件路径非法");
        }
        return candidate;
    }
}
