package com.brushing.web.controller.member;

import java.util.List;

import com.brushing.common.core.domain.entity.SysUser;
import com.brushing.common.utils.StringUtils;
import com.brushing.member.domain.OrderMemberUser;
import com.brushing.member.domain.OrderWithdrawal;
import com.brushing.member.service.IOrderMemberUserService;
import com.brushing.member.service.IOrderWithdrawalService;
import com.brushing.system.service.ISysUserService;
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
import com.brushing.member.domain.OrderInfo;
import com.brushing.member.service.IOrderInfoService;
import com.brushing.common.utils.poi.ExcelUtil;
import com.brushing.common.core.page.TableDataInfo;

/**
 * 订单列表Controller
 * 
 * @author brushing
 * @date 2025-08-02
 */
@RestController
@RequestMapping("/member/orderInfo")
public class OrderInfoController extends BaseController
{
    @Autowired
    private IOrderInfoService orderInfoService;

    @Autowired
    private IOrderWithdrawalService withdrawalService;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private IOrderMemberUserService orderMemberUserService;
    /**
     * 查询订单列表列表
     */
    @PreAuthorize("@ss.hasPermi('member:orderInfo:list')")
    @GetMapping("/list")
    public TableDataInfo list(OrderInfo orderInfo)
    {
        Long userId = getUserId();
        SysUser sysUser = userService.selectUserById(userId);
        if (sysUser.getUserName().equals("admin")){
            startPage();
            List<OrderInfo> list = orderInfoService.selectOrderInfoList(orderInfo);
            return getDataTable(list);
        }
        String agentUser = sysUser.getAgentUser();
        if (StringUtils.isEmpty(agentUser)){
            startPage();
            List<OrderInfo> list = orderInfoService.selectOrderInfoList(orderInfo);
            return getDataTable(list);
        }
        OrderMemberUser byUsername = orderMemberUserService.findByUsername(agentUser);
        if (StringUtils.isNull(byUsername)){
            startPage();
            List<OrderInfo> list = orderInfoService.selectOrderInfoList(orderInfo);
            return getDataTable(list);
        }
        startPage();
        orderInfo.setAgentUserId(byUsername.getId());
        List<OrderInfo> list = orderInfoService.selectOrderInfoList(orderInfo);
        return getDataTable(list);
    }

    /**
     * 导出订单列表列表
     */
    @PreAuthorize("@ss.hasPermi('member:orderInfo:export')")
    @Log(title = "订单列表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, OrderInfo orderInfo)
    {
        List<OrderInfo> list = orderInfoService.selectOrderInfoList(orderInfo);
        ExcelUtil<OrderInfo> util = new ExcelUtil<OrderInfo>(OrderInfo.class);
        util.exportExcel(response, list, "订单列表数据");
    }

    /**
     * 获取订单列表详细信息
     */
    @PreAuthorize("@ss.hasPermi('member:orderInfo:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(orderInfoService.selectOrderInfoById(id));
    }

    /**
     * 新增订单列表
     */
    @PreAuthorize("@ss.hasPermi('member:orderInfo:add')")
    @Log(title = "订单列表", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody OrderInfo orderInfo)
    {
        return toAjax(orderInfoService.insertOrderInfo(orderInfo));
    }

    /**
     * 修改订单列表
     */
    @PreAuthorize("@ss.hasPermi('member:orderInfo:edit')")
    @Log(title = "订单列表", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody OrderInfo orderInfo)
    {
        return toAjax(orderInfoService.updateOrderInfo(orderInfo));
    }

    /**
     * 删除订单列表
     */
    @PreAuthorize("@ss.hasPermi('member:orderInfo:remove')")
    @Log(title = "订单列表", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(orderInfoService.deleteOrderInfoByIds(ids));
    }


    @GetMapping("/getStatusOneCount")
    public AjaxResult getStatusOneCount(){
        int i = orderInfoService.countStatusOneInOrderInfo();
        int i1 = withdrawalService.countStatusOneInOrderWithdrawal();
        AjaxResult ajaxResult =new AjaxResult();
        ajaxResult.put("orderCount",i);
        ajaxResult.put("withdrawalCount",i1);
        return  success(ajaxResult);
    }
}
