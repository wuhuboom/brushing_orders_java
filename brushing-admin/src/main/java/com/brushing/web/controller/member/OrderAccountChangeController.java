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
import com.brushing.member.domain.OrderAccountChange;
import com.brushing.member.service.IOrderAccountChangeService;
import com.brushing.common.utils.poi.ExcelUtil;
import com.brushing.common.core.page.TableDataInfo;

/**
 * 账户变动Controller
 * 
 * @author brushing
 * @date 2025-08-02
 */
@RestController
@RequestMapping("/member/change")
public class OrderAccountChangeController extends BaseController
{
    @Autowired
    private IOrderAccountChangeService orderAccountChangeService;

    /**
     * 查询账户变动列表
     */
    @PreAuthorize("@ss.hasPermi('member:change:list')")
    @GetMapping("/list")
    public TableDataInfo list(OrderAccountChange orderAccountChange)
    {
        startPage();
        List<OrderAccountChange> list = orderAccountChangeService.selectOrderAccountChangeList(orderAccountChange);
        return getDataTable(list);
    }

    /**
     * 导出账户变动列表
     */
    @PreAuthorize("@ss.hasPermi('member:change:export')")
    @Log(title = "账户变动", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, OrderAccountChange orderAccountChange)
    {
        List<OrderAccountChange> list = orderAccountChangeService.selectOrderAccountChangeList(orderAccountChange);
        ExcelUtil<OrderAccountChange> util = new ExcelUtil<OrderAccountChange>(OrderAccountChange.class);
        util.exportExcel(response, list, "账户变动数据");
    }

    /**
     * 获取账户变动详细信息
     */
    @PreAuthorize("@ss.hasPermi('member:change:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return success(orderAccountChangeService.selectOrderAccountChangeById(id));
    }

    /**
     * 新增账户变动
     */
    @PreAuthorize("@ss.hasPermi('member:change:add')")
    @Log(title = "账户变动", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody OrderAccountChange orderAccountChange)
    {
        return toAjax(orderAccountChangeService.insertOrderAccountChange(orderAccountChange));
    }

    /**
     * 修改账户变动
     */
    @PreAuthorize("@ss.hasPermi('member:change:edit')")
    @Log(title = "账户变动", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody OrderAccountChange orderAccountChange)
    {
        return toAjax(orderAccountChangeService.updateOrderAccountChange(orderAccountChange));
    }

    /**
     * 删除账户变动
     */
    @PreAuthorize("@ss.hasPermi('member:change:remove')")
    @Log(title = "账户变动", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(orderAccountChangeService.deleteOrderAccountChangeByIds(ids));
    }
}
