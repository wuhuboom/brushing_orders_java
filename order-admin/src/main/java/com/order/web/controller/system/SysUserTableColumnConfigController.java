package com.order.web.controller.system;

import jakarta.validation.Valid;
import com.order.common.core.controller.BaseController;
import com.order.common.core.domain.AjaxResult;
import com.order.system.domain.dto.TableColumnConfigDto;
import com.order.system.service.ISysUserTableColumnConfigService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前后台用户的表格列配置。
 */
@RestController
@RequestMapping("/system/user/table-column-config")
public class SysUserTableColumnConfigController extends BaseController
{
    private final ISysUserTableColumnConfigService configService;

    public SysUserTableColumnConfigController(ISysUserTableColumnConfigService configService)
    {
        this.configService = configService;
    }

    /**
     * 查询当前用户的表格列配置。未配置时成功响应不包含data。
     */
    @GetMapping("/{tableKey}")
    public AjaxResult get(@PathVariable String tableKey)
    {
        return success(configService.selectTableColumnConfig(getUserId(), tableKey));
    }

    /**
     * 新增或更新当前用户的表格列配置。
     */
    @PutMapping("/{tableKey}")
    public AjaxResult save(
            @PathVariable String tableKey,
            @Valid @RequestBody TableColumnConfigDto config)
    {
        configService.saveTableColumnConfig(getUserId(), getUsername(), tableKey, config);
        return success();
    }

    /**
     * 重置当前用户的表格列配置。
     */
    @DeleteMapping("/{tableKey}")
    public AjaxResult remove(@PathVariable String tableKey)
    {
        configService.deleteTableColumnConfig(getUserId(), tableKey);
        return success();
    }
}
