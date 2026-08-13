package com.brushing.web.controller.member;

import java.util.List;

import com.brushing.common.core.domain.entity.SysUser;
import com.brushing.common.utils.DateUtils;
import com.brushing.common.utils.StringUtils;
import com.brushing.member.domain.OrderMemberUser;
import com.brushing.member.service.IOrderMemberUserService;
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
import com.brushing.member.domain.OrderTopup;
import com.brushing.member.service.IOrderTopupService;
import com.brushing.common.utils.poi.ExcelUtil;
import com.brushing.common.core.page.TableDataInfo;

/**
 * 充值记录Controller
 * 
 * @author brushing
 * @date 2025-08-04
 */
@RestController
@RequestMapping("/member/topup")
public class OrderTopupController extends BaseController
{
    @Autowired
    private IOrderTopupService orderTopupService;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private IOrderMemberUserService orderMemberUserService;

    /**
     * 查询充值记录列表
     */
    @PreAuthorize("@ss.hasPermi('member:topup:list')")
    @GetMapping("/list")
    public TableDataInfo list(OrderTopup orderTopup)
    {
        Long userId = getUserId();
        SysUser sysUser = userService.selectUserById(userId);
        if (sysUser.getUserName().equals("admin")){
            startPage();
            List<OrderTopup> list = orderTopupService.selectOrderTopupList(orderTopup);
            return getDataTable(list);
        }
        String agentUser = sysUser.getAgentUser();
        if (StringUtils.isEmpty(agentUser)){
            startPage();
            List<OrderTopup> list = orderTopupService.selectOrderTopupList(orderTopup);
            return getDataTable(list);
        }
        OrderMemberUser byUsername = orderMemberUserService.findByUsername(agentUser);
        if (StringUtils.isNull(byUsername)){
            startPage();
            List<OrderTopup> list = orderTopupService.selectOrderTopupList(orderTopup);
            return getDataTable(list);
        }
        startPage();
        orderTopup.setAgentUserId(byUsername.getId());
        List<OrderTopup> list = orderTopupService.selectOrderTopupList(orderTopup);
        return getDataTable(list);
    }

    /**
     * 导出充值记录列表
     */
    @PreAuthorize("@ss.hasPermi('member:topup:export')")
    @Log(title = "充值记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, OrderTopup orderTopup)
    {
        List<OrderTopup> list = orderTopupService.selectOrderTopupList(orderTopup);
        ExcelUtil<OrderTopup> util = new ExcelUtil<OrderTopup>(OrderTopup.class);
        util.exportExcel(response, list, "充值记录数据");
    }

    /**
     * 获取充值记录详细信息
     */
    @PreAuthorize("@ss.hasPermi('member:topup:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(orderTopupService.selectOrderTopupById(id));
    }

    /**
     * 新增充值记录
     */
    @PreAuthorize("@ss.hasPermi('member:topup:add')")
    @Log(title = "充值记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody OrderTopup orderTopup)
    {
        return toAjax(orderTopupService.insertOrderTopup(orderTopup));
    }

    /**
     * 修改充值记录
     */
    @PreAuthorize("@ss.hasPermi('member:topup:edit')")
    @Log(title = "充值记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody OrderTopup orderTopup)
    {
        OrderTopup orderTopup1 = orderTopupService.selectOrderTopupById(orderTopup.getId());
        if (!orderTopup1.getStatus().equals("1")){
            return AjaxResult.error("不能重复审核！");
        }
        if (orderTopup.getStatus().equals("0") &&(orderTopup.getRealMoney()==null || orderTopup.getRealMoney().doubleValue()<=0)){
            return AjaxResult.error("请输入实际金额");
        }
        orderTopup.setAuditTime(DateUtils.getNowDate());
        orderTopup.setAuditor(getUsername());
        if(orderTopup.getStatus().equals("0")){
            orderTopup.setRealMoney(orderTopup.getRealMoney());
        }else if(orderTopup.getStatus().equals("2")){
            orderTopup.setRealMoney(null);
        }
        return toAjax(orderTopupService.updateOrderTopup(orderTopup));
    }

    /**
     * 删除充值记录
     */
    @PreAuthorize("@ss.hasPermi('member:topup:remove')")
    @Log(title = "充值记录", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(orderTopupService.deleteOrderTopupByIds(ids));
    }
}
