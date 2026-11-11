package com.order.web.controller.member;

import com.order.common.annotation.Log;
import com.order.common.core.domain.AjaxResult;
import com.order.common.enums.BusinessType;
import com.order.web.service.ConfigImageImportService;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Batch image import for VIP levels and the single event/certificate configuration. */
@RestController
@RequestMapping("/member/orderconfig/images")
public class ConfigImageImportController {
    private final ConfigImageImportService importService;

    public ConfigImageImportController(ConfigImageImportService importService) {
        this.importService = importService;
    }

    @PreAuthorize("@ss.hasAnyPermi('member:level:edit,member:orderconfig:edit')")
    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AjaxResult preview(@RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "append") String richTextMode) {
        return AjaxResult.success(importService.preview(file, richTextMode));
    }

    @PreAuthorize("@ss.hasAnyPermi('member:level:edit,member:orderconfig:edit')")
    @Log(title = "批量导入配置图片", businessType = BusinessType.IMPORT)
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AjaxResult importImages(@RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "append") String richTextMode) {
        ConfigImageImportService.ImportResult result = importService.importImages(file, richTextMode);
        return result.valid() ? AjaxResult.success("图片导入成功", result)
                : AjaxResult.success("图片导入校验未通过", result);
    }
}
