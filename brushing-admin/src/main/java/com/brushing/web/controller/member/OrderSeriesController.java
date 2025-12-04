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
import com.brushing.common.utils.MessageUtils;
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
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(orderSeriesService.selectOrderSeriesById(id));
    }

    /**
     * 新增连单
     */
    @Log(title = "连单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody OrderSeries orderSeries)
    {
        orderSeries.setCreateBy(getUsername());
        int i = orderSeriesService.insertOrderSeries(orderSeries);
        if (i==5){
            return error(MessageUtils.message("series.already_set"));
        }
        if (i==6){
            return error(MessageUtils.message("series.product_data_error"));
        }
        return toAjax(i);
    }

    @PostMapping("/addSeries")
    public AjaxResult addSeries(@RequestBody List<OrderSeries> orderSeries){

        return toAjax(orderSeriesService.insertOrderSeries(orderSeries));
    }

    /**
     * 修改连单
     */
    @Log(title = "连单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody OrderSeries orderSeries)
    {
        OrderSeries series = orderSeriesService.selectOrderSeriesById(orderSeries.getId());
        if (!series.getStatus().equals("1")){
            return error(MessageUtils.message("series.cannot_modify"));
        }
        return toAjax(orderSeriesService.updateOrderSeries(orderSeries));
    }

    /**
     * 删除连单
     */
    @Log(title = "连单", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
       for (int i=0;i<ids.length;i++){
           OrderSeries series = orderSeriesService.selectOrderSeriesById(ids[i]);
           if (!series.getStatus().equals("1")){
               return error(MessageUtils.message("series.cannot_delete"));
           }
       }
        int i = orderSeriesService.deleteOrderSeriesByIds(ids);
        return toAjax(i);
    }
}
