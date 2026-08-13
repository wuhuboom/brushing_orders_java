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
import com.brushing.member.domain.OrderFieldSetting;
import com.brushing.member.service.IOrderFieldSettingService;
import com.brushing.common.utils.poi.ExcelUtil;
import com.brushing.common.core.page.TableDataInfo;
import com.brushing.common.utils.MessageUtils;

/**
 * 字段设置Controller
 *
 * @author brushing
 * @date 2026-01-26
 */
@RestController
@RequestMapping("/member/field")
public class OrderFieldSettingController extends BaseController
{
    @Autowired
    private IOrderFieldSettingService orderFieldSettingService;

    /**
     * 查询字段设置列表
     */
    @PreAuthorize("@ss.hasPermi('member:field:list')")
    @GetMapping("/list")
    public TableDataInfo list(OrderFieldSetting orderFieldSetting)
    {
        startPage();
        List<OrderFieldSetting> list = orderFieldSettingService.selectOrderFieldSettingList(orderFieldSetting);
        return getDataTable(list);
    }

    /**
     * 导出字段设置列表
     */
    @PreAuthorize("@ss.hasPermi('member:field:export')")
    @Log(title = "字段设置", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, OrderFieldSetting orderFieldSetting)
    {
        List<OrderFieldSetting> list = orderFieldSettingService.selectOrderFieldSettingList(orderFieldSetting);
        ExcelUtil<OrderFieldSetting> util = new ExcelUtil<OrderFieldSetting>(OrderFieldSetting.class);
        util.exportExcel(response, list, "字段设置数据");
    }

    /**
     * 获取字段设置详细信息
     */
    @PreAuthorize("@ss.hasPermi('member:field:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(orderFieldSettingService.selectOrderFieldSettingById(id));
    }

    /**
     * 新增字段设置
     */
    @PreAuthorize("@ss.hasPermi('member:field:add')")
    @Log(title = "字段设置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody OrderFieldSetting orderFieldSetting)
    {
        if (!orderFieldSettingService.checkTypeUnique(orderFieldSetting))
        {
            return error(MessageUtils.message("field.add.type_exists", orderFieldSetting.getType()));
        }
        return toAjax(orderFieldSettingService.insertOrderFieldSetting(orderFieldSetting));
    }

    /**
     * 修改字段设置
     */
    @PreAuthorize("@ss.hasPermi('member:field:edit')")
    @Log(title = "字段设置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody OrderFieldSetting orderFieldSetting)
    {
        if (!orderFieldSettingService.checkTypeUnique(orderFieldSetting))
        {
            return error(MessageUtils.message("field.edit.type_exists", orderFieldSetting.getType()));
        }
        return toAjax(orderFieldSettingService.updateOrderFieldSetting(orderFieldSetting));
    }

    /**
     * 删除字段设置
     */
    @PreAuthorize("@ss.hasPermi('member:field:remove')")
    @Log(title = "字段设置", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(orderFieldSettingService.deleteOrderFieldSettingByIds(ids));
    }
}
