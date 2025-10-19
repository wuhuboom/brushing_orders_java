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
import com.brushing.member.domain.OrderLotteryConfig;
import com.brushing.member.service.IOrderLotteryConfigService;
import com.brushing.common.utils.poi.ExcelUtil;
import com.brushing.common.core.page.TableDataInfo;

/**
 * 抽奖配置Controller
 * 
 * @author brushing
 * @date 2025-10-12
 */
@RestController
@RequestMapping("/member/lotteryconfig")
public class OrderLotteryConfigController extends BaseController
{
    @Autowired
    private IOrderLotteryConfigService orderLotteryConfigService;

    /**
     * 查询抽奖配置列表
     */
    @PreAuthorize("@ss.hasPermi('member:lotteryconfig:list')")
    @GetMapping("/list")
    public TableDataInfo list(OrderLotteryConfig orderLotteryConfig)
    {
        startPage();
        List<OrderLotteryConfig> list = orderLotteryConfigService.selectOrderLotteryConfigList(orderLotteryConfig);
        return getDataTable(list);
    }

    /**
     * 导出抽奖配置列表
     */
    @PreAuthorize("@ss.hasPermi('member:lotteryconfig:export')")
    @Log(title = "抽奖配置", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, OrderLotteryConfig orderLotteryConfig)
    {
        List<OrderLotteryConfig> list = orderLotteryConfigService.selectOrderLotteryConfigList(orderLotteryConfig);
        ExcelUtil<OrderLotteryConfig> util = new ExcelUtil<OrderLotteryConfig>(OrderLotteryConfig.class);
        util.exportExcel(response, list, "抽奖配置数据");
    }

    /**
     * 获取抽奖配置详细信息
     */
    @PreAuthorize("@ss.hasPermi('member:lotteryconfig:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(orderLotteryConfigService.selectOrderLotteryConfigById(id));
    }

    /**
     * 新增抽奖配置
     */
    @PreAuthorize("@ss.hasPermi('member:lotteryconfig:add')")
    @Log(title = "抽奖配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody OrderLotteryConfig orderLotteryConfig)
    {
        return toAjax(orderLotteryConfigService.insertOrderLotteryConfig(orderLotteryConfig));
    }

    /**
     * 修改抽奖配置
     */
    @PreAuthorize("@ss.hasPermi('member:lotteryconfig:edit')")
    @Log(title = "抽奖配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody OrderLotteryConfig orderLotteryConfig)
    {
        return toAjax(orderLotteryConfigService.updateOrderLotteryConfig(orderLotteryConfig));
    }

    /**
     * 删除抽奖配置
     */
    @PreAuthorize("@ss.hasPermi('member:lotteryconfig:remove')")
    @Log(title = "抽奖配置", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(orderLotteryConfigService.deleteOrderLotteryConfigByIds(ids));
    }
}
