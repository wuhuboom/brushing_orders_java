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
import com.order.member.domain.GoodsTransactionFlow;
import com.order.member.service.IGoodsTransactionFlowService;
import com.order.common.utils.poi.ExcelUtil;
import com.order.common.core.page.TableDataInfo;

/**
 * 交易流水Controller
 * 
 * @author order
 * @date 2025-10-27
 */
@RestController
@RequestMapping("/member/flow")
public class GoodsTransactionFlowController extends BaseController
{
    @Autowired
    private IGoodsTransactionFlowService goodsTransactionFlowService;

    /**
     * 查询交易流水列表
     */
    @PreAuthorize("@ss.hasPermi('member:flow:list')")
    @GetMapping("/list")
    public TableDataInfo list(GoodsTransactionFlow goodsTransactionFlow)
    {
        startPage();
        List<GoodsTransactionFlow> list = goodsTransactionFlowService.selectGoodsTransactionFlowList(goodsTransactionFlow);
        return getDataTable(list);
    }

    /**
     * 导出交易流水列表
     */
    @PreAuthorize("@ss.hasPermi('member:flow:export')")
    @Log(title = "交易流水", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, GoodsTransactionFlow goodsTransactionFlow)
    {
        List<GoodsTransactionFlow> list = goodsTransactionFlowService.selectGoodsTransactionFlowList(goodsTransactionFlow);
        ExcelUtil<GoodsTransactionFlow> util = new ExcelUtil<GoodsTransactionFlow>(GoodsTransactionFlow.class);
        util.exportExcel(response, list, "交易流水数据");
    }

    /**
     * 获取交易流水详细信息
     */
    @PreAuthorize("@ss.hasPermi('member:flow:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(goodsTransactionFlowService.selectGoodsTransactionFlowById(id));
    }

    /**
     * 新增交易流水
     */
    @PreAuthorize("@ss.hasPermi('member:flow:add')")
    @Log(title = "交易流水", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody GoodsTransactionFlow goodsTransactionFlow)
    {
        return toAjax(goodsTransactionFlowService.insertGoodsTransactionFlow(goodsTransactionFlow));
    }

    /**
     * 修改交易流水
     */
    @PreAuthorize("@ss.hasPermi('member:flow:edit')")
    @Log(title = "交易流水", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody GoodsTransactionFlow goodsTransactionFlow)
    {
        return toAjax(goodsTransactionFlowService.updateGoodsTransactionFlow(goodsTransactionFlow));
    }

    /**
     * 删除交易流水
     */
    @PreAuthorize("@ss.hasPermi('member:flow:remove')")
    @Log(title = "交易流水", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(goodsTransactionFlowService.deleteGoodsTransactionFlowByIds(ids));
    }
}
