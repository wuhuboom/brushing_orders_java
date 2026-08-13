package com.order.api.controller;

import com.order.api.service.ApiLocaleService;
import com.order.api.service.ConfigQueryService;
import com.order.api.service.LocalizedMessageCatalogService;
import com.order.common.config.OrderConfig;
import com.order.common.core.domain.AjaxResult;
import com.order.common.i18n.SupportedLocale;
import com.order.common.utils.file.FileUploadUtils;
import com.order.common.utils.file.ImageUploadValidator;
import com.order.common.utils.file.MimeTypeUtils;
import com.order.framework.config.ServerConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "前台配置")
@RestController
@RequestMapping("/api/config")
public class ConfigController {
    private static final Logger log = LoggerFactory.getLogger(ConfigController.class);
    private final ConfigQueryService queryService;
    private final ApiLocaleService localeService;
    private final LocalizedMessageCatalogService messageCatalogService;
    private final ServerConfig serverConfig;

    public ConfigController(
            ConfigQueryService queryService,
            ApiLocaleService localeService,
            LocalizedMessageCatalogService messageCatalogService,
            ServerConfig serverConfig) {
        this.queryService = queryService;
        this.localeService = localeService;
        this.messageCatalogService = messageCatalogService;
        this.serverConfig = serverConfig;
    }

    @GetMapping("/error-messages")
    @Operation(summary = "获取当前语言的前台业务码文案")
    public ResponseEntity<AjaxResult> errorMessages(
            @RequestParam(required = false) String lang,
            HttpServletRequest request) {
        SupportedLocale locale = localeService.resolve(lang, request);
        return canonical(success(locale, messageCatalogService.catalog(locale)), locale);
    }

    @GetMapping("/content")
    @Operation(summary = "获取本地化公共内容")
    public ResponseEntity<AjaxResult> content(
            @RequestParam(required = false) String lang,
            HttpServletRequest request) {
        SupportedLocale locale = localeService.resolve(lang, request);
        return canonical(success(locale, queryService.content(locale)), locale);
    }

    @GetMapping("/website")
    @Operation(summary = "获取公开 H5 网站展示配置")
    public ResponseEntity<AjaxResult> website(
            @RequestParam(required = false) String lang,
            HttpServletRequest request) {
        SupportedLocale locale = localeService.resolve(lang, request);
        return canonical(success(locale, queryService.website()), locale);
    }

    @GetMapping("/customer-services")
    @Operation(summary = "获取本地化客服")
    public ResponseEntity<AjaxResult> customerServices(
            @RequestParam(required = false) String lang,
            HttpServletRequest request) {
        SupportedLocale locale = localeService.resolve(lang, request);
        return canonical(success(locale, queryService.customerServices(locale)), locale);
    }

    @GetMapping("/member-levels")
    @Operation(summary = "获取本地化VIP等级")
    public ResponseEntity<AjaxResult> memberLevels(
            @RequestParam(required = false) String lang,
            HttpServletRequest request) {
        SupportedLocale locale = localeService.resolve(lang, request);
        return canonical(success(locale, queryService.memberLevels(locale)), locale);
    }

    @GetMapping("/time-zone")
    @Operation(summary = "获取系统时区")
    public ResponseEntity<AjaxResult> timeZone(
            @RequestParam(required = false) String lang,
            HttpServletRequest request) {
        SupportedLocale locale = localeService.resolve(lang, request);
        return canonical(success(locale, queryService.timeZone()), locale);
    }

    @GetMapping("/trade")
    @Operation(summary = "获取客户端交易配置")
    public ResponseEntity<AjaxResult> trade(
            @RequestParam(required = false) String lang,
            HttpServletRequest request) {
        SupportedLocale locale = localeService.resolve(lang, request);
        return canonical(success(locale, queryService.trade()), locale);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传提现账户附件")
    public ResponseEntity<AjaxResult> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String lang,
            HttpServletRequest request) {
        SupportedLocale locale = localeService.resolve(lang, request);
        try {
            ImageUploadValidator.validate(file);
            String fileName = FileUploadUtils.upload(
                    OrderConfig.getUploadPath(), file, MimeTypeUtils.IMAGE_EXTENSION, true);
            String url = serverConfig.getUrl() + fileName;
            AjaxResult result = success(locale, null);
            result.put("fileName", fileName);
            result.put("url", url);
            return canonical(result, locale);
        } catch (ImageUploadValidator.InvalidImageUploadException exception) {
            log.warn("event=config_upload_rejected contentType={} size={}",
                    file == null ? null : file.getContentType(),
                    file == null ? null : file.getSize());
            return uploadFailure(locale);
        } catch (Exception exception) {
            log.error("event=config_upload_failed", exception);
            return uploadFailure(locale);
        }
    }

    private ResponseEntity<AjaxResult> uploadFailure(SupportedLocale locale) {
        return ResponseEntity.ok()
                .headers(localeService.responseHeaders(locale))
                .body(AjaxResult.error(703, "Upload failed"));
    }

    private AjaxResult success(SupportedLocale locale, Object data) {
        return AjaxResult.success("Success", data);
    }

    private <T> ResponseEntity<T> canonical(T body, SupportedLocale locale) {
        return ResponseEntity.ok().headers(localeService.responseHeaders(locale)).body(body);
    }

}
