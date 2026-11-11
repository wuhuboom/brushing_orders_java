package com.order.web.service;

import com.order.common.utils.file.FileUploadUtils;
import com.order.framework.config.ServerConfig;
import com.order.system.service.ISystemAlignmentService;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Uses the same local storage and managed-file index as /common/upload. */
@Component
public class ConfigImageImportStorage {
    private final ServerConfig serverConfig;
    private final ISystemAlignmentService alignmentService;

    public ConfigImageImportStorage(ServerConfig serverConfig, ISystemAlignmentService alignmentService) {
        this.serverConfig = serverConfig;
        this.alignmentService = alignmentService;
    }

    public StoredImage store(String fileName, String extension, byte[] bytes, String group, Long targetId) {
        String uploadDir = com.order.common.config.OrderConfig.getUploadPath();
        String relative = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
                + "/" + UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path destination = Path.of(uploadDir, relative);
        try {
            Files.createDirectories(destination.getParent());
            Files.write(destination, bytes, StandardOpenOption.CREATE_NEW);
            String publicPath = FileUploadUtils.getPathFileName(uploadDir, relative);
            String url = serverConfig.getUrl() + publicPath;
            Map<String, Object> data = new HashMap<>();
            data.put("bucket", "local");
            data.put("fileType", extension);
            data.put("contentType", "image/" + ("jpg".equals(extension) ? "jpeg" : extension));
            data.put("storagePath", destination.toString());
            data.put("fileUrl", url);
            String normalizedName = fileName.replace('\\', '/');
            data.put("referenceName", normalizedName.substring(normalizedName.lastIndexOf('/') + 1));
            data.put("referenceType", "vip".equals(group) ? "MEMBER_LEVEL" : "CONFIG_IMAGE");
            data.put("referenceTargetId", String.valueOf(targetId));
            Map<String, Object> indexed = alignmentService.registerFile(data);
            return new StoredImage(String.valueOf(indexed.getOrDefault("fileUrl", url)), destination);
        } catch (Exception exception) {
            delete(destination);
            throw new IllegalStateException("图片保存失败，请稍后重试", exception);
        }
    }

    public void delete(Path destination) {
        try {
            Files.deleteIfExists(destination);
        } catch (IOException exception) {
            // An unreferenced newly generated file is preferable to deleting a shared indexed file.
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Unable to remove rolled-back import file", exception);
        }
    }

    public record StoredImage(String url, Path writtenPath) {
    }
}
