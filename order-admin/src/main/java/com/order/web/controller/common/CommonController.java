package com.order.web.controller.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.order.common.config.OrderConfig;
import com.order.common.core.domain.AjaxResult;
import com.order.common.utils.StringUtils;
import com.order.common.utils.file.FileUploadUtils;
import com.order.common.utils.file.FileUtils;
import com.order.framework.config.ServerConfig;
import com.order.system.service.ISystemAlignmentService;

/**
 * 通用请求处理
 * 
 * @author order
 */
@RestController
@RequestMapping("/common")
public class CommonController
{
    private static final Logger log = LoggerFactory.getLogger(CommonController.class);

    @Autowired
    private ServerConfig serverConfig;

    @Autowired
    private ISystemAlignmentService systemAlignmentService;

    private static final String FILE_DELIMETER = ",";

    /**
     * 通用下载请求
     * 
     * @param fileName 文件名称
     * @param delete 是否删除
     */
    @GetMapping("/download")
    public void fileDownload(String fileName, Boolean delete, HttpServletResponse response, HttpServletRequest request)
    {
        try
        {
            if (!FileUtils.checkAllowDownload(fileName))
            {
                throw new Exception(StringUtils.format("文件名称({})非法，不允许下载。 ", fileName));
            }
            String realFileName = System.currentTimeMillis() + fileName.substring(fileName.indexOf("_") + 1);
            String filePath = OrderConfig.getDownloadPath() + fileName;

            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            FileUtils.setAttachmentResponseHeader(response, realFileName);
            FileUtils.writeBytes(filePath, response.getOutputStream());
            if (delete)
            {
                FileUtils.deleteFile(filePath);
            }
        }
        catch (Exception e)
        {
            log.error("下载文件失败", e);
        }
    }

    /**
     * 通用上传请求（单个）
     */
    @PostMapping("/upload")
    public AjaxResult uploadFile(MultipartFile file, String referenceType, String referenceTargetId,
            Integer relatedKind, String relatedId) throws Exception
    {
        try
        {
            // 上传文件路径
            String filePath = OrderConfig.getUploadPath();
            // 上传并返回新文件名称
            String fileName = FileUploadUtils.upload(filePath, file);
            String url = serverConfig.getUrl() + fileName;
            Map<String, Object> indexedFile = indexUpload(file, fileName, url,
                    normalizeReferenceType(referenceType, relatedKind), firstText(referenceTargetId, relatedId));
            AjaxResult ajax = AjaxResult.success();
            ajax.put("url", indexedFile.getOrDefault("fileUrl", url));
            ajax.put("fileName", indexedFile.getOrDefault("publicPath", fileName));
            ajax.put("newFileName", FileUtils.getName(fileName));
            ajax.put("originalFilename", file.getOriginalFilename());
            ajax.put("fileId", indexedFile.get("fileId"));
            ajax.put("referenceId", indexedFile.get("referenceId"));
            return ajax;
        }
        catch (Exception e)
        {
            log.error("上传文件失败", e);
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 通用上传请求（多个）
     */
    @PostMapping("/uploads")
    public AjaxResult uploadFiles(List<MultipartFile> files, String referenceType, String referenceTargetId,
            Integer relatedKind, String relatedId) throws Exception
    {
        try
        {
            // 上传文件路径
            String filePath = OrderConfig.getUploadPath();
            List<String> urls = new ArrayList<String>();
            List<String> fileNames = new ArrayList<String>();
            List<String> newFileNames = new ArrayList<String>();
            List<String> originalFilenames = new ArrayList<String>();
            List<Long> fileIds = new ArrayList<Long>();
            List<Long> referenceIds = new ArrayList<Long>();
            for (MultipartFile file : files)
            {
                // 上传并返回新文件名称
                String fileName = FileUploadUtils.upload(filePath, file);
                String url = serverConfig.getUrl() + fileName;
                Map<String, Object> indexedFile = indexUpload(file, fileName, url,
                        normalizeReferenceType(referenceType, relatedKind), firstText(referenceTargetId, relatedId));
                urls.add(String.valueOf(indexedFile.getOrDefault("fileUrl", url)));
                fileNames.add(String.valueOf(indexedFile.getOrDefault("publicPath", fileName)));
                newFileNames.add(FileUtils.getName(fileName));
                originalFilenames.add(file.getOriginalFilename());
                fileIds.add(((Number) indexedFile.get("fileId")).longValue());
                referenceIds.add(((Number) indexedFile.get("referenceId")).longValue());
            }
            AjaxResult ajax = AjaxResult.success();
            ajax.put("urls", StringUtils.join(urls, FILE_DELIMETER));
            ajax.put("fileNames", StringUtils.join(fileNames, FILE_DELIMETER));
            ajax.put("newFileNames", StringUtils.join(newFileNames, FILE_DELIMETER));
            ajax.put("originalFilenames", StringUtils.join(originalFilenames, FILE_DELIMETER));
            ajax.put("fileIds", fileIds);
            ajax.put("referenceIds", referenceIds);
            return ajax;
        }
        catch (Exception e)
        {
            log.error("批量上传文件失败", e);
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 本地资源通用下载
     */
    @GetMapping("/download/resource")
    public void resourceDownload(String resource, HttpServletRequest request, HttpServletResponse response)
            throws Exception
    {
        try
        {
            if (!FileUtils.checkAllowDownload(resource))
            {
                throw new Exception(StringUtils.format("资源文件({})非法，不允许下载。 ", resource));
            }
            // 本地资源路径
            String localPath = OrderConfig.getProfile();
            // 数据库资源地址
            String downloadPath = localPath + FileUtils.stripPrefix(resource);
            // 下载名称
            String downloadName = StringUtils.substringAfterLast(downloadPath, "/");
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            FileUtils.setAttachmentResponseHeader(response, downloadName);
            FileUtils.writeBytes(downloadPath, response.getOutputStream());
        }
        catch (Exception e)
        {
            log.error("下载文件失败", e);
        }
    }

    private Map<String, Object> indexUpload(MultipartFile file, String publicPath, String url,
            String referenceType, String referenceTargetId)
    {
        Map<String, Object> data = new HashMap<>();
        data.put("bucket", "local");
        data.put("fileType", StringUtils.substringAfterLast(file.getOriginalFilename(), "."));
        data.put("contentType", file.getContentType());
        data.put("storagePath", OrderConfig.getProfile() + FileUtils.stripPrefix(publicPath));
        data.put("fileUrl", url);
        data.put("referenceName", file.getOriginalFilename());
        data.put("referenceType", referenceType);
        data.put("referenceTargetId", referenceTargetId);
        Map<String, Object> indexed = systemAlignmentService.registerFile(data);
        Object indexedUrl = indexed.get("fileUrl");
        if (indexedUrl != null)
        {
            String value = String.valueOf(indexedUrl);
            indexed.put("publicPath", value.startsWith(serverConfig.getUrl())
                    ? value.substring(serverConfig.getUrl().length()) : publicPath);
        }
        return indexed;
    }

    private String normalizeReferenceType(String referenceType, Integer relatedKind)
    {
        if (StringUtils.isNotEmpty(referenceType))
        {
            return referenceType;
        }
        if (relatedKind == null)
        {
            return "ADMIN_UPLOAD";
        }
        return switch (relatedKind)
        {
            case 1 -> "USER_AVATAR";
            case 2 -> "MEMBER_AVATAR";
            case 3 -> "ADMIN_UPLOAD";
            case 4 -> "GOODS_CATEGORY";
            case 5 -> "GOODS";
            case 6 -> "BANNER";
            case 7 -> "CUSTOMER_SERVICE";
            case 8 -> "MEMBER_LEVEL";
            case 9 -> "POINTS_GIFT";
            case 10 -> "ACTIVITY";
            default -> "ADMIN_UPLOAD";
        };
    }

    private String firstText(String first, String second)
    {
        return StringUtils.isNotEmpty(first) ? first : second;
    }
}
