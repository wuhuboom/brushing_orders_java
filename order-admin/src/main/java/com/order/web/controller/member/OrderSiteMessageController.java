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
import com.order.member.domain.OrderSiteMessage;
import com.order.member.service.IOrderSiteMessageService;
import com.order.common.utils.poi.ExcelUtil;
import com.order.common.core.page.TableDataInfo;

/**
 * 站内信Controller
 * 
 * @author order
 * @date 2025-11-10
 */
@RestController
@RequestMapping("/member/message")
public class OrderSiteMessageController extends BaseController
{
    @Autowired
    private IOrderSiteMessageService orderSiteMessageService;

    /**
     * 查询站内信列表
     */
    @PreAuthorize("@ss.hasPermi('member:message:list')")
    @GetMapping("/list")
    public TableDataInfo list(OrderSiteMessage orderSiteMessage)
    {
        startPage();
        List<OrderSiteMessage> list = orderSiteMessageService.selectOrderSiteMessageList(orderSiteMessage);
        return getDataTable(list);
    }

    /**
     * 导出站内信列表
     */
    @PreAuthorize("@ss.hasPermi('member:message:export')")
    @Log(title = "站内信", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, OrderSiteMessage orderSiteMessage)
    {
        List<OrderSiteMessage> list = orderSiteMessageService.selectOrderSiteMessageList(orderSiteMessage);
        ExcelUtil<OrderSiteMessage> util = new ExcelUtil<OrderSiteMessage>(OrderSiteMessage.class);
        util.exportExcel(response, list, "站内信数据");
    }

    /**
     * 获取站内信详细信息
     */
    @PreAuthorize("@ss.hasPermi('member:message:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(orderSiteMessageService.selectOrderSiteMessageById(id));
    }

    /**
     * 新增站内信
     */
    @PreAuthorize("@ss.hasPermi('member:message:add')")
    @Log(title = "站内信", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody OrderSiteMessage orderSiteMessage)
    {
        return toAjax(orderSiteMessageService.insertOrderSiteMessage(orderSiteMessage));
    }

    /**
     * 修改站内信
     */
    @PreAuthorize("@ss.hasPermi('member:message:edit')")
    @Log(title = "站内信", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody OrderSiteMessage orderSiteMessage)
    {
        return toAjax(orderSiteMessageService.updateOrderSiteMessage(orderSiteMessage));
    }

    /**
     * 删除站内信
     */
    @PreAuthorize("@ss.hasPermi('member:message:remove')")
    @Log(title = "站内信", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(orderSiteMessageService.deleteOrderSiteMessageByIds(ids));
    }
}
