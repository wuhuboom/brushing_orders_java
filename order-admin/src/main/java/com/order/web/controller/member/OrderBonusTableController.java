package com.order.web.controller.member;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
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
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(orderBonusTableService.selectOrderBonusTableById(id));
    }

    /**
     * 新增彩金
     */
    @Log(title = "彩金", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody OrderBonusTable orderBonusTable)
    {
        return toAjax(orderBonusTableService.insertOrderBonusTable(orderBonusTable));
    }

    /**
     * 修改彩金
     */
    @Log(title = "彩金", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody OrderBonusTable orderBonusTable)
    {
        return toAjax(orderBonusTableService.updateOrderBonusTable(orderBonusTable));
    }

    /**
     * 删除彩金
     */
    @Log(title = "彩金", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(orderBonusTableService.deleteOrderBonusTableByIds(ids));
    }
}
