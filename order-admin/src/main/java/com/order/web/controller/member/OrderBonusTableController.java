package com.order.web.controller.member;

import java.util.List;

import com.order.common.utils.StringUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
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
import com.order.member.domain.OrderBonusTable;
import com.order.member.service.IOrderBonusTableService;
import com.order.common.utils.poi.ExcelUtil;
import com.order.common.core.page.TableDataInfo;

/**
 * 彩金Controller
 * 
 * @author order
 * @date 2025-11-04
 */
@RestController
@RequestMapping("/member/bonus")
public class OrderBonusTableController extends BaseController
{
    @Autowired
    private IOrderBonusTableService orderBonusTableService;

    /**
     * 查询彩金列表
     */
    @PreAuthorize("@ss.hasPermi('member:bonus:list')")
    @GetMapping("/list")
    public TableDataInfo list(OrderBonusTable orderBonusTable)
    {
        startPage();
        List<OrderBonusTable> list = orderBonusTableService.selectOrderBonusTableList(orderBonusTable);
        return getDataTable(list);
    }

    /**
     * 导出彩金列表
     */
    @Log(title = "彩金", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('member:bonus:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, OrderBonusTable orderBonusTable)
    {
        List<OrderBonusTable> list = orderBonusTableService.selectOrderBonusTableList(orderBonusTable);
        ExcelUtil<OrderBonusTable> util = new ExcelUtil<OrderBonusTable>(OrderBonusTable.class);
        util.exportExcel(response, list, "彩金数据");
    }

    /**
     * 获取彩金详细信息
     */
    @PreAuthorize("@ss.hasPermi('member:bonus:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(orderBonusTableService.selectOrderBonusTableById(id));
    }

    /**
     * 新增彩金
     */
    @Log(title = "彩金", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('member:bonus:add')")
    @PostMapping
    public AjaxResult add(@RequestBody OrderBonusTable orderBonusTable)
    {
        OrderBonusTable orderBonusTable1 = orderBonusTable.getOrderNum() == null
                ? null
                : orderBonusTableService.userHaveBonus(
                        orderBonusTable.getUserId(),
                        orderBonusTable.getOrderNum().intValue());
        if (StringUtils.isNotNull(orderBonusTable1)) {
            return error("该用户的当前单数已存在未领取彩金，不能重复添加");
        }
        return toAjax(orderBonusTableService.insertOrderBonusTable(orderBonusTable));
    }

    /**
     * 修改彩金
     */
    @Log(title = "彩金", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('member:bonus:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody OrderBonusTable orderBonusTable)
    {
        return toAjax(orderBonusTableService.updateOrderBonusTable(orderBonusTable));
    }

    @Log(title = "彩金领取", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('member:bonus:receive')")
    @PutMapping("/{id}/receive")
    public AjaxResult receive(@PathVariable Long id)
    {
        return toAjax(orderBonusTableService.receiveBonus(id));
    }

    @Log(title = "彩金发放", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('member:bonus:give')")
    @PutMapping("/{id}/given")
    public AjaxResult given(@PathVariable Long id)
    {
        return toAjax(orderBonusTableService.distributeBonus(id));
    }

    /**
     * 删除彩金
     */
    @Log(title = "彩金", businessType = BusinessType.DELETE)
	@PreAuthorize("@ss.hasPermi('member:bonus:remove')")
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(orderBonusTableService.deleteOrderBonusTableByIds(ids));
    }
}
