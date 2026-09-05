package com.order.web.controller.system;

import com.order.common.annotation.Log;
import com.order.common.core.controller.BaseController;
import com.order.common.core.domain.AjaxResult;
import com.order.common.core.page.TableDataInfo;
import com.order.common.enums.BusinessType;
import com.order.common.utils.poi.ExcelUtil;
import com.order.system.domain.SysTimeZone;
import com.order.system.service.ISysTimeZoneService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 时区管理Controller
 * 
 * @author betting
 * @date 2025-08-26
 */
@RestController
@RequestMapping("/system/zone")
public class SysTimeZoneController extends BaseController
{
    @Autowired
    private ISysTimeZoneService sysTimeZoneService;

    /**
     * 查询时区管理列表
     */
    @PreAuthorize("@ss.hasPermi('system:zone:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysTimeZone sysTimeZone)
    {
        startPage();
        List<SysTimeZone> list = sysTimeZoneService.selectSysTimeZoneList(sysTimeZone);
        return getDataTable(list);
    }

    /**
     * 导出时区管理列表
     */
    @PreAuthorize("@ss.hasPermi('system:zone:export')")
    @Log(title = "时区管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysTimeZone sysTimeZone)
    {
        List<SysTimeZone> list = sysTimeZoneService.selectSysTimeZoneList(sysTimeZone);
        ExcelUtil<SysTimeZone> util = new ExcelUtil<SysTimeZone>(SysTimeZone.class);
        util.exportExcel(response, list, "时区管理数据");
    }

    /**
     * 获取时区管理详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:zone:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(sysTimeZoneService.selectSysTimeZoneById(id));
    }

    /**
     * 新增时区管理
     */
    @PreAuthorize("@ss.hasPermi('system:zone:add')")
    @Log(title = "时区管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysTimeZone sysTimeZone)
    {
        return toAjax(sysTimeZoneService.insertSysTimeZone(sysTimeZone));
    }

    //选择时区
    @PreAuthorize("@ss.hasPermi('system:zone:active')")
    @PostMapping("/active/{id}")
    public AjaxResult setActive(@PathVariable Long id) {

        return toAjax(sysTimeZoneService.setActiveById(id));
    }

    @GetMapping("/getActive")
    public AjaxResult getActive(){
        SysTimeZone active = sysTimeZoneService.getActive();
        AjaxResult ajaxResult=new AjaxResult();
        ajaxResult.put("data",active);
        ajaxResult.put("code",200);
        ajaxResult.put("msg","success");
        return ajaxResult;
    }

    /**
     * 修改时区管理
     */
    @PreAuthorize("@ss.hasPermi('system:zone:edit')")
    @Log(title = "时区管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysTimeZone sysTimeZone)
    {
        return toAjax(sysTimeZoneService.updateSysTimeZone(sysTimeZone));
    }

    /**
     * 删除时区管理
     */
    @PreAuthorize("@ss.hasPermi('system:zone:remove')")
    @Log(title = "时区管理", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(sysTimeZoneService.deleteSysTimeZoneByIds(ids));
    }
}
