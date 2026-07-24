package com.order.api.controller;

import com.order.api.controller.dto.ConfigApiDtos;
import com.order.api.controller.dto.PageDto;
import com.order.api.service.ApiLocaleService;
import com.order.api.service.ConfigApiException;
import com.order.api.service.ConfigQueryService;
import com.order.api.service.LocalizedApiMessageService;
import com.order.api.service.LocalizedMessageCatalogService;
import com.order.common.config.OrderConfig;
import com.order.common.core.domain.AjaxResult;
import com.order.common.core.page.TableDataInfo;
import com.order.common.i18n.SupportedLocale;
import com.order.common.utils.file.FileUploadUtils;
import com.order.common.utils.file.ImageUploadValidator;
import com.order.common.utils.file.MimeTypeUtils;
import com.order.framework.config.ServerConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "前台配置")
@RestController
@RequestMapping("/api/config")
public class ConfigController {
    private static final Logger log = LoggerFactory.getLogger(ConfigController.class);
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final ConfigQueryService queryService;
    private final ApiLocaleService localeService;
    private final LocalizedApiMessageService messageService;
    private final LocalizedMessageCatalogService messageCatalogService;
    private final ServerConfig serverConfig;

    public ConfigController(
            ConfigQueryService queryService,
            ApiLocaleService localeService,
            LocalizedApiMessageService messageService,
            LocalizedMessageCatalogService messageCatalogService,
            ServerConfig serverConfig) {
        this.queryService = queryService;
        this.localeService = localeService;
        this.messageService = messageService;
        this.messageCatalogService = messageCatalogService;
        this.serverConfig = serverConfig;
    }

    @GetMapping("/languages")
    @Operation(summary = "获取支持的语言")
    public ResponseEntity<AjaxResult> languages(
            @RequestParam(required = false) String lang,
            HttpServletRequest request) {
        SupportedLocale locale = localeService.resolve(lang, request);
        return canonical(success(locale, queryService.languages()), locale);
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

    @GetMapping("/notices")
    @Operation(summary = "获取本地化公告列表")
    public ResponseEntity<TableDataInfo> notices(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String lang,
            HttpServletRequest request) {
        validatePage(pageNum, pageSize);
        SupportedLocale locale = localeService.resolve(lang, request);
        return canonical(page(locale, queryService.notices(locale, pageNum, pageSize)), locale);
    }

    @GetMapping("/notices/{id}")
    @Operation(summary = "获取本地化公告详情")
    public ResponseEntity<AjaxResult> notice(
            @PathVariable Long id,
            @RequestParam(required = false) String lang,
            HttpServletRequest request) {
        SupportedLocale locale = localeService.resolve(lang, request);
        return canonical(success(locale, queryService.notice(id, locale)), locale);
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

    @GetMapping("/getGlobalConfig")
    @Operation(summary = "兼容：获取基本配置", deprecated = true)
    public ResponseEntity<AjaxResult> legacyGlobalConfig(
            @RequestParam(required = false) String lang,
            HttpServletRequest request) {
        SupportedLocale locale = localeService.resolve(lang, request);
        return legacy(success(locale, queryService.content(locale)), locale, "/api/config/content");
    }

    @GetMapping("/getConfigByLang")
    @Operation(summary = "兼容：获取本地化内容", deprecated = true)
    public ResponseEntity<AjaxResult> legacyConfigByLang(
            @RequestParam(required = false) String lang,
            HttpServletRequest request) {
        return legacyGlobalConfig(lang, request);
    }

    @GetMapping("/getCustomerService")
    @Operation(summary = "兼容：获取客服", deprecated = true)
    public ResponseEntity<AjaxResult> legacyCustomerService(
            @RequestParam(required = false) String lang,
            HttpServletRequest request) {
        SupportedLocale locale = localeService.resolve(lang, request);
        List<ConfigApiDtos.CustomerServiceResponse> services =
                queryService.customerServices(locale);
        AjaxResult result = services.isEmpty()
                ? AjaxResult.error(701, messageService.message(701, locale, "No data"))
                : success(locale, services);
        return legacy(result, locale, "/api/config/customer-services");
    }

    @GetMapping("/getCustomerServiceByLang")
    @Operation(summary = "兼容：获取本地化客服", deprecated = true)
    public ResponseEntity<AjaxResult> legacyCustomerServiceByLang(
            @RequestParam(required = false) String lang,
        HttpServletRequest request) {
        SupportedLocale locale = localeService.resolve(lang, request);
        List<ConfigApiDtos.CustomerServiceResponse> services =
                queryService.customerServices(locale);
        AjaxResult result = services.isEmpty()
                ? AjaxResult.error(701, messageService.message(701, locale, "No data"))
                : success(locale, services);
        result.put("lang", locale.getCode());
        return legacy(result, locale, "/api/config/customer-services");
    }

    @GetMapping("/getTradeConfig")
    @Operation(summary = "兼容：获取客户端交易配置", deprecated = true)
    public ResponseEntity<AjaxResult> legacyTrade(
            @RequestParam(required = false) String lang,
            HttpServletRequest request) {
        SupportedLocale locale = localeService.resolve(lang, request);
        return legacy(success(locale, queryService.trade()), locale, "/api/config/trade");
    }

    @GetMapping("/getLevel")
    @Operation(summary = "兼容：获取VIP等级", deprecated = true)
    public ResponseEntity<AjaxResult> legacyLevels(
            @RequestParam(required = false) String lang,
            HttpServletRequest request) {
        SupportedLocale locale = localeService.resolve(lang, request);
        return legacy(success(locale, queryService.memberLevels(locale)), locale, "/api/config/member-levels");
    }

    @GetMapping("/getNoticeList")
    @Operation(summary = "兼容：获取公告列表", deprecated = true)
    public ResponseEntity<TableDataInfo> legacyNotices(
            @Valid PageDto dto,
            @RequestParam(required = false) String lang,
            HttpServletRequest request) {
        int pageNum = dto == null || dto.getPageNum() == null ? 1 : dto.getPageNum();
        int pageSize = dto == null || dto.getPageSize() == null ? DEFAULT_PAGE_SIZE : dto.getPageSize();
        validatePage(pageNum, pageSize);
        SupportedLocale locale = localeService.resolve(lang, request);
        return legacy(
                page(locale, queryService.notices(locale, pageNum, pageSize)),
                locale,
                "/api/config/notices");
    }

    @GetMapping("/getNotice/{id}")
    @Operation(summary = "兼容：获取公告详情", deprecated = true)
    public ResponseEntity<AjaxResult> legacyNotice(
            @PathVariable Long id,
            @RequestParam(required = false) String lang,
            HttpServletRequest request) {
        SupportedLocale locale = localeService.resolve(lang, request);
        return legacy(success(locale, queryService.notice(id, locale)), locale, "/api/config/notices/" + id);
    }

    @GetMapping("/getZoneActive")
    @Operation(summary = "兼容：获取系统时区", deprecated = true)
    public ResponseEntity<AjaxResult> legacyTimeZone(
            @RequestParam(required = false) String lang,
            HttpServletRequest request) {
        SupportedLocale locale = localeService.resolve(lang, request);
        return legacy(success(locale, queryService.timeZone()), locale, "/api/config/time-zone");
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "兼容：上传文件", deprecated = true)
    public ResponseEntity<AjaxResult> legacyUpload(
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
            return legacy(result, locale, "/api/user/avatar");
        } catch (ImageUploadValidator.InvalidImageUploadException exception) {
            log.warn("event=legacy_config_upload_rejected contentType={} size={}",
                    file == null ? null : file.getContentType(),
                    file == null ? null : file.getSize());
            return uploadFailure(locale);
        } catch (Exception exception) {
            log.error("event=legacy_config_upload_failed", exception);
            return uploadFailure(locale);
        }
    }

    private ResponseEntity<AjaxResult> uploadFailure(SupportedLocale locale) {
        String message = messageService.message(703, locale, "Upload failed");
        return ResponseEntity.ok()
                .headers(localeService.legacyHeaders(locale, "/api/user/avatar"))
                .body(AjaxResult.error(703, message));
    }

    private AjaxResult success(SupportedLocale locale, Object data) {
        return AjaxResult.success(messageService.message(200, locale, "Success"), data);
    }

    private TableDataInfo page(
            SupportedLocale locale,
            ConfigApiDtos.PagedResult<?> result) {
        TableDataInfo table = new TableDataInfo();
        table.setCode(200);
        table.setMsg(messageService.message(200, locale, "Success"));
        table.setRows(result.rows());
        table.setTotal(result.total());
        return table;
    }

    private void validatePage(int pageNum, int pageSize) {
        if (pageNum < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new ConfigApiException(400, org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid pagination");
        }
    }

    private <T> ResponseEntity<T> canonical(T body, SupportedLocale locale) {
        return ResponseEntity.ok().headers(localeService.responseHeaders(locale)).body(body);
    }

    private <T> ResponseEntity<T> legacy(
            T body,
            SupportedLocale locale,
            String successorPath) {
        return ResponseEntity.ok().headers(localeService.legacyHeaders(locale, successorPath)).body(body);
    }
}
