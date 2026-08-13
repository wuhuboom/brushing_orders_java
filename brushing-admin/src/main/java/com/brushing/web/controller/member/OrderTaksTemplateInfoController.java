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
import com.brushing.member.domain.OrderTaksTemplateInfo;
import com.brushing.member.service.IOrderTaksTemplateInfoService;
import com.brushing.common.utils.poi.ExcelUtil;
import com.brushing.common.core.page.TableDataInfo;

/**
 * 连单模板任务
Controller
 *
 * @author brushing
 * @date 2026-01-10
 */
@RestController
@RequestMapping("/member/templateInfo")
public class OrderTaksTemplateInfoController extends BaseController
{
    @Autowired
    private IOrderTaksTemplateInfoService orderTaksTemplateInfoService;

    /**
     * 查询连单模板任务
列表
     */
    @PreAuthorize("@ss.hasPermi('member:templateInfo:list')")
    @GetMapping("/list")
    public TableDataInfo list(OrderTaksTemplateInfo orderTaksTemplateInfo)
    {
        startPage();
        List<OrderTaksTemplateInfo> list = orderTaksTemplateInfoService.selectOrderTaksTemplateInfoList(orderTaksTemplateInfo);
        return getDataTable(list);
    }

    /**
     * 导出连单模板任务
列表
     */
    @PreAuthorize("@ss.hasPermi('member:templateInfo:export')")
    @Log(title = "连单模板任务", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, OrderTaksTemplateInfo orderTaksTemplateInfo)
    {
        List<OrderTaksTemplateInfo> list = orderTaksTemplateInfoService.selectOrderTaksTemplateInfoList(orderTaksTemplateInfo);
        ExcelUtil<OrderTaksTemplateInfo> util = new ExcelUtil<OrderTaksTemplateInfo>(OrderTaksTemplateInfo.class);
        util.exportExcel(response, list, "连单模板任务数据");
    }

    /**
     * 获取连单模板任务
详细信息
     */
    @PreAuthorize("@ss.hasPermi('member:templateInfo:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(orderTaksTemplateInfoService.selectOrderTaksTemplateInfoById(id));
    }

    /**
     * 新增连单模板任务

     */
    @PreAuthorize("@ss.hasPermi('member:templateInfo:add')")
    @Log(title = "连单模板任务", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody OrderTaksTemplateInfo orderTaksTemplateInfo)
    {
        return toAjax(orderTaksTemplateInfoService.insertOrderTaksTemplateInfo(orderTaksTemplateInfo));
    }

    /**
     * 修改连单模板任务

     */
    @PreAuthorize("@ss.hasPermi('member:templateInfo:edit')")
    @Log(title = "连单模板任务", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody OrderTaksTemplateInfo orderTaksTemplateInfo)
    {
        return toAjax(orderTaksTemplateInfoService.updateOrderTaksTemplateInfo(orderTaksTemplateInfo));
    }

    /**
     * 删除连单模板任务

     */
    @PreAuthorize("@ss.hasPermi('member:templateInfo:remove')")
    @Log(title = "连单模板任务", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(orderTaksTemplateInfoService.deleteOrderTaksTemplateInfoByIds(ids));
    }
}
