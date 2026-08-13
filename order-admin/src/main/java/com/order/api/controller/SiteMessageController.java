package com.order.api.controller;

import com.order.api.controller.dto.ConfigApiDtos;
import com.order.api.service.ApiLocaleService;
import com.order.api.service.SiteMessageQueryService;
import com.order.common.core.domain.AjaxResult;
import com.order.common.core.page.TableDataInfo;
import com.order.common.i18n.SupportedLocale;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "会员站内信")
@RestController
@RequestMapping("/api/messages")
public class SiteMessageController {
    private final SiteMessageQueryService queryService;
    private final ApiLocaleService localeService;

    public SiteMessageController(
            SiteMessageQueryService queryService,
            ApiLocaleService localeService) {
        this.queryService = queryService;
        this.localeService = localeService;
    }

    @GetMapping
    @Operation(summary = "查询当前会员的站内信")
    public ResponseEntity<TableDataInfo> list(
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String lang,
            HttpServletRequest request) {
        SupportedLocale locale = localeService.resolve(lang, request);
        ConfigApiDtos.PagedResult<ConfigApiDtos.NoticeSummaryResponse> result =
                queryService.list(userId, locale, pageNum, pageSize);
        TableDataInfo table = new TableDataInfo();
        table.setCode(200);
        table.setMsg("Success");
        table.setRows(result.rows());
        table.setTotal(result.total());
        return ResponseEntity.ok()
                .headers(localeService.responseHeaders(locale))
                .body(table);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询当前会员的站内信详情")
    public ResponseEntity<AjaxResult> get(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id,
            @RequestParam(required = false) String lang,
            HttpServletRequest request) {
        SupportedLocale locale = localeService.resolve(lang, request);
        AjaxResult result = AjaxResult.success(
                "Success",
                queryService.get(userId, id, locale));
        return ResponseEntity.ok()
                .headers(localeService.responseHeaders(locale))
                .body(result);
    }
}
