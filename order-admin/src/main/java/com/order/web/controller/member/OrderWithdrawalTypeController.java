package com.order.web.controller.member;

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
import com.order.common.annotation.Log;
import com.order.common.core.controller.BaseController;
import com.order.common.core.domain.AjaxResult;
import com.order.common.enums.BusinessType;
import com.order.member.domain.OrderWithdrawalType;
import com.order.member.service.IOrderWithdrawalTypeService;
import com.order.common.utils.poi.ExcelUtil;
import com.order.common.core.page.TableDataInfo;

/**
 * 出金类型Controller
 * 
 * @author order
 * @date 2025-11-14
 */
@RestController
@RequestMapping("/member/withdrawaltype")
public class OrderWithdrawalTypeController extends BaseController
{
    @Autowired
    private IOrderWithdrawalTypeService orderWithdrawalTypeService;

    /**
     * 查询出金类型列表
     */
    @PreAuthorize("@ss.hasPermi('member:withdrawaltype:list')")
    @GetMapping("/list")
    public TableDataInfo list(OrderWithdrawalType orderWithdrawalType)
    {
        startPage();
        List<OrderWithdrawalType> list = orderWithdrawalTypeService.selectOrderWithdrawalTypeList(orderWithdrawalType);
        return getDataTable(list);
    }

    /**
     * 导出出金类型列表
     */
    @PreAuthorize("@ss.hasPermi('member:withdrawaltype:export')")
    @Log(title = "出金类型", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, OrderWithdrawalType orderWithdrawalType)
    {
        List<OrderWithdrawalType> list = orderWithdrawalTypeService.selectOrderWithdrawalTypeList(orderWithdrawalType);
        ExcelUtil<OrderWithdrawalType> util = new ExcelUtil<OrderWithdrawalType>(OrderWithdrawalType.class);
        util.exportExcel(response, list, "出金类型数据");
    }

    /**
     * 获取出金类型详细信息
     */
    @PreAuthorize("@ss.hasPermi('member:withdrawaltype:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(orderWithdrawalTypeService.selectOrderWithdrawalTypeById(id));
    }

    /**
     * 新增出金类型
     */
    @PreAuthorize("@ss.hasPermi('member:withdrawaltype:add')")
    @Log(title = "出金类型", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody OrderWithdrawalType orderWithdrawalType)
    {
        return toAjax(orderWithdrawalTypeService.insertOrderWithdrawalType(orderWithdrawalType));
    }

    /**
     * 修改出金类型
     */
    @PreAuthorize("@ss.hasPermi('member:withdrawaltype:edit')")
    @Log(title = "出金类型", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody OrderWithdrawalType orderWithdrawalType)
    {
        return toAjax(orderWithdrawalTypeService.updateOrderWithdrawalType(orderWithdrawalType));
    }

    /**
     * 删除出金类型
     */
    @PreAuthorize("@ss.hasPermi('member:withdrawaltype:remove')")
    @Log(title = "出金类型", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(orderWithdrawalTypeService.deleteOrderWithdrawalTypeByIds(ids));
    }
}
