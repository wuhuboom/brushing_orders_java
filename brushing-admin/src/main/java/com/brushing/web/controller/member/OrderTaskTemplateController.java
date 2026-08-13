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
import com.brushing.member.domain.OrderTaskTemplate;
import com.brushing.member.service.IOrderTaskTemplateService;
import com.brushing.common.utils.poi.ExcelUtil;
import com.brushing.common.core.page.TableDataInfo;

/**
 * 连单模板Controller
 *
 * @author brushing
 * @date 2026-01-10
 */
@RestController
@RequestMapping("/member/template")
public class OrderTaskTemplateController extends BaseController
{
    @Autowired
    private IOrderTaskTemplateService orderTaskTemplateService;

    /**
     * 查询连单模板列表
     */
    @PreAuthorize("@ss.hasPermi('member:template:list')")
    @GetMapping("/list")
    public TableDataInfo list(OrderTaskTemplate orderTaskTemplate)
    {
        startPage();
        List<OrderTaskTemplate> list = orderTaskTemplateService.selectOrderTaskTemplateList(orderTaskTemplate);
        return getDataTable(list);
    }


    @GetMapping("/options")
    public AjaxResult getTemplateOptions()
    {
        List<OrderTaskTemplate> list = orderTaskTemplateService.selectOrderTaskTemplateList(new OrderTaskTemplate());
        return AjaxResult.success(list);
    }

    /**
     * 导出连单模板列表
     */
    @PreAuthorize("@ss.hasPermi('member:template:export')")
    @Log(title = "连单模板", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, OrderTaskTemplate orderTaskTemplate)
    {
        List<OrderTaskTemplate> list = orderTaskTemplateService.selectOrderTaskTemplateList(orderTaskTemplate);
        ExcelUtil<OrderTaskTemplate> util = new ExcelUtil<OrderTaskTemplate>(OrderTaskTemplate.class);
        util.exportExcel(response, list, "连单模板数据");
    }

    /**
     * 获取连单模板详细信息
     */
    @PreAuthorize("@ss.hasPermi('member:template:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(orderTaskTemplateService.selectOrderTaskTemplateById(id));
    }

    /**
     * 新增连单模板
     */
    @PreAuthorize("@ss.hasPermi('member:template:add')")
    @Log(title = "连单模板", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody OrderTaskTemplate orderTaskTemplate)
    {
        return toAjax(orderTaskTemplateService.insertOrderTaskTemplate(orderTaskTemplate));
    }

    /**
     * 修改连单模板
     */
    @PreAuthorize("@ss.hasPermi('member:template:edit')")
    @Log(title = "连单模板", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody OrderTaskTemplate orderTaskTemplate)
    {
        return toAjax(orderTaskTemplateService.updateOrderTaskTemplate(orderTaskTemplate));
    }

    /**
     * 删除连单模板
     */
    @PreAuthorize("@ss.hasPermi('member:template:remove')")
    @Log(title = "连单模板", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(orderTaskTemplateService.deleteOrderTaskTemplateByIds(ids));
    }
}
