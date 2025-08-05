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
import com.brushing.member.domain.OrderSeries;
import com.brushing.member.service.IOrderSeriesService;
import com.brushing.common.utils.poi.ExcelUtil;
import com.brushing.common.core.page.TableDataInfo;

/**
 * 连单Controller
 * 
 * @author brushing
 * @date 2025-08-04
 */
@RestController
@RequestMapping("/member/series")
public class OrderSeriesController extends BaseController
{
    @Autowired
    private IOrderSeriesService orderSeriesService;

    /**
     * 查询连单列表
     */
    @PreAuthorize("@ss.hasPermi('member:series:list')")
    @GetMapping("/list")
    public TableDataInfo list(OrderSeries orderSeries)
    {
        startPage();
        List<OrderSeries> list = orderSeriesService.selectOrderSeriesList(orderSeries);
        return getDataTable(list);
    }

    /**
     * 导出连单列表
     */
    @PreAuthorize("@ss.hasPermi('member:series:export')")
    @Log(title = "连单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, OrderSeries orderSeries)
    {
        List<OrderSeries> list = orderSeriesService.selectOrderSeriesList(orderSeries);
        ExcelUtil<OrderSeries> util = new ExcelUtil<OrderSeries>(OrderSeries.class);
        util.exportExcel(response, list, "连单数据");
    }

    /**
     * 获取连单详细信息
     */
    @PreAuthorize("@ss.hasPermi('member:series:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(orderSeriesService.selectOrderSeriesById(id));
    }

    /**
     * 新增连单
     */
    @PreAuthorize("@ss.hasPermi('member:series:add')")
    @Log(title = "连单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody OrderSeries orderSeries)
    {
        orderSeries.setCreateBy(getUsername());
        int i = orderSeriesService.insertOrderSeries(orderSeries);
        if (i==5){
            return error("已设置连单");
        }
        if (i==6){
            return error("商品数据错误");
        }
        return toAjax(i);
    }

    /**
     * 修改连单
     */
    @PreAuthorize("@ss.hasPermi('member:series:edit')")
    @Log(title = "连单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody OrderSeries orderSeries)
    {
        OrderSeries series = orderSeriesService.selectOrderSeriesById(orderSeries.getId());
        if (!series.getStatus().equals("1")){
            return error("该订单已无法修改");
        }
        return toAjax(orderSeriesService.updateOrderSeries(orderSeries));
    }

    /**
     * 删除连单
     */
    @PreAuthorize("@ss.hasPermi('member:series:remove')")
    @Log(title = "连单", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
       for (int i=0;i<ids.length;i++){
           OrderSeries series = orderSeriesService.selectOrderSeriesById(ids[i]);
           if (!series.getStatus().equals("1")){
               return error("订单已无法删除");
           }
       }
        int i = orderSeriesService.deleteOrderSeriesByIds(ids);
        return toAjax(i);
    }
}
