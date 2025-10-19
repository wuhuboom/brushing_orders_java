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
import com.brushing.member.domain.OrderLotteryRecord;
import com.brushing.member.service.IOrderLotteryRecordService;
import com.brushing.common.utils.poi.ExcelUtil;
import com.brushing.common.core.page.TableDataInfo;

/**
 * 抽奖记录Controller
 * 
 * @author brushing
 * @date 2025-10-12
 */
@RestController
@RequestMapping("/member/looteryrecord")
public class OrderLotteryRecordController extends BaseController
{
    @Autowired
    private IOrderLotteryRecordService orderLotteryRecordService;

    /**
     * 查询抽奖记录列表
     */
    @PreAuthorize("@ss.hasPermi('member:looteryrecord:list')")
    @GetMapping("/list")
    public TableDataInfo list(OrderLotteryRecord orderLotteryRecord)
    {
        startPage();
        List<OrderLotteryRecord> list = orderLotteryRecordService.selectOrderLotteryRecordList(orderLotteryRecord);
        return getDataTable(list);
    }

    /**
     * 导出抽奖记录列表
     */
    @PreAuthorize("@ss.hasPermi('member:looteryrecord:export')")
    @Log(title = "抽奖记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, OrderLotteryRecord orderLotteryRecord)
    {
        List<OrderLotteryRecord> list = orderLotteryRecordService.selectOrderLotteryRecordList(orderLotteryRecord);
        ExcelUtil<OrderLotteryRecord> util = new ExcelUtil<OrderLotteryRecord>(OrderLotteryRecord.class);
        util.exportExcel(response, list, "抽奖记录数据");
    }

    /**
     * 获取抽奖记录详细信息
     */
    @PreAuthorize("@ss.hasPermi('member:looteryrecord:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(orderLotteryRecordService.selectOrderLotteryRecordById(id));
    }

    /**
     * 新增抽奖记录
     */
    @PreAuthorize("@ss.hasPermi('member:looteryrecord:add')")
    @Log(title = "抽奖记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody OrderLotteryRecord orderLotteryRecord)
    {
        return toAjax(orderLotteryRecordService.insertOrderLotteryRecord(orderLotteryRecord));
    }

    /**
     * 修改抽奖记录
     */
    @PreAuthorize("@ss.hasPermi('member:looteryrecord:edit')")
    @Log(title = "抽奖记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody OrderLotteryRecord orderLotteryRecord)
    {
        return toAjax(orderLotteryRecordService.updateOrderLotteryRecord(orderLotteryRecord));
    }

    /**
     * 删除抽奖记录
     */
    @PreAuthorize("@ss.hasPermi('member:looteryrecord:remove')")
    @Log(title = "抽奖记录", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(orderLotteryRecordService.deleteOrderLotteryRecordByIds(ids));
    }
}
