package com.brushing.web.controller.member;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.brushing.common.annotation.Log;
import com.brushing.common.core.controller.BaseController;
import com.brushing.common.core.domain.AjaxResult;
import com.brushing.common.enums.BusinessType;
import com.brushing.member.domain.WelfareConfig;
import com.brushing.member.service.IWelfareConfigService;
import com.brushing.common.utils.poi.ExcelUtil;
import com.brushing.common.core.page.TableDataInfo;

/**
 * 福利配置Controller
 * 
 * @author brushing
 * @date 2025-10-18
 */
@RestController
@RequestMapping("/member/walfare")
public class WelfareConfigController extends BaseController
{
    @Autowired
    private IWelfareConfigService welfareConfigService;

    /**
     * 查询福利配置列表
     */
    @PreAuthorize("@ss.hasPermi('member:walfare:list')")
    @GetMapping("/list")
    public TableDataInfo list(WelfareConfig welfareConfig)
    {
        startPage();
        List<WelfareConfig> list = welfareConfigService.selectWelfareConfigList(welfareConfig);
        return getDataTable(list);
    }

    /**
     * 导出福利配置列表
     */
    @PreAuthorize("@ss.hasPermi('member:walfare:export')")
    @Log(title = "福利配置", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WelfareConfig welfareConfig)
    {
        List<WelfareConfig> list = welfareConfigService.selectWelfareConfigList(welfareConfig);
        ExcelUtil<WelfareConfig> util = new ExcelUtil<WelfareConfig>(WelfareConfig.class);
        util.exportExcel(response, list, "福利配置数据");
    }

    /**
     * 获取福利配置详细信息
     */
    @PreAuthorize("@ss.hasPermi('member:walfare:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(welfareConfigService.selectWelfareConfigById(id));
    }

    /**
     * 新增福利配置
     */
    @PreAuthorize("@ss.hasPermi('member:walfare:add')")
    @Log(title = "福利配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WelfareConfig welfareConfig)
    {
        return toAjax(welfareConfigService.insertWelfareConfig(welfareConfig));
    }

    /**
     * 修改福利配置
     */
    @PreAuthorize("@ss.hasPermi('member:walfare:edit')")
    @Log(title = "福利配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WelfareConfig welfareConfig)
    {
        return toAjax(welfareConfigService.updateWelfareConfig(welfareConfig));
    }

    /**
     * 删除福利配置
     */
    @PreAuthorize("@ss.hasPermi('member:walfare:remove')")
    @Log(title = "福利配置", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(welfareConfigService.deleteWelfareConfigByIds(ids));
    }
}
