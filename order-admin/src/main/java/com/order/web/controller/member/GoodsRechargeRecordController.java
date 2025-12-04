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
import com.order.member.domain.GoodsRechargeRecord;
import com.order.member.service.IGoodsRechargeRecordService;
import com.order.common.utils.poi.ExcelUtil;
import com.order.common.core.page.TableDataInfo;

/**
 * 充值记录Controller
 * 
 * @author order
 * @date 2025-10-27
 */
@RestController
@RequestMapping("/member/recharge")
public class GoodsRechargeRecordController extends BaseController
{
    @Autowired
    private IGoodsRechargeRecordService goodsRechargeRecordService;

    /**
     * 查询充值记录列表
     */
    @PreAuthorize("@ss.hasPermi('member:recharge:list')")
    @GetMapping("/list")
    public TableDataInfo list(GoodsRechargeRecord goodsRechargeRecord)
    {
        startPage();
        List<GoodsRechargeRecord> list = goodsRechargeRecordService.selectGoodsRechargeRecordList(goodsRechargeRecord);
        return getDataTable(list);
    }

    /**
     * 导出充值记录列表
     */
    @PreAuthorize("@ss.hasPermi('member:recharge:export')")
    @Log(title = "充值记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, GoodsRechargeRecord goodsRechargeRecord)
    {
        List<GoodsRechargeRecord> list = goodsRechargeRecordService.selectGoodsRechargeRecordList(goodsRechargeRecord);
        ExcelUtil<GoodsRechargeRecord> util = new ExcelUtil<GoodsRechargeRecord>(GoodsRechargeRecord.class);
        util.exportExcel(response, list, "充值记录数据");
    }

    /**
     * 获取充值记录详细信息
     */
    @PreAuthorize("@ss.hasPermi('member:recharge:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(goodsRechargeRecordService.selectGoodsRechargeRecordById(id));
    }

    /**
     * 新增充值记录
     */
    @PreAuthorize("@ss.hasPermi('member:recharge:add')")
    @Log(title = "充值记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody GoodsRechargeRecord goodsRechargeRecord)
    {
        return toAjax(goodsRechargeRecordService.insertGoodsRechargeRecord(goodsRechargeRecord));
    }

    /**
     * 修改充值记录
     */
    @PreAuthorize("@ss.hasPermi('member:recharge:edit')")
    @Log(title = "充值记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody GoodsRechargeRecord goodsRechargeRecord)
    {
        return toAjax(goodsRechargeRecordService.updateGoodsRechargeRecord(goodsRechargeRecord));
    }

    /**
     * 删除充值记录
     */
    @PreAuthorize("@ss.hasPermi('member:recharge:remove')")
    @Log(title = "充值记录", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(goodsRechargeRecordService.deleteGoodsRechargeRecordByIds(ids));
    }
}
