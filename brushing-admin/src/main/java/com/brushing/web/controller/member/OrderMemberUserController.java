package com.brushing.web.controller.member;

import java.util.List;

import com.brushing.common.utils.SecurityUtils;
import com.brushing.common.utils.StringUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
import com.brushing.member.domain.OrderMemberUser;
import com.brushing.member.service.IOrderMemberUserService;
import com.brushing.common.utils.poi.ExcelUtil;
import com.brushing.common.core.page.TableDataInfo;

/**
 * 会员用户Controller
 * 
 * @author brushing
 * @date 2025-07-30
 */
@RestController
@RequestMapping("/member/member")
public class OrderMemberUserController extends BaseController
{
    @Autowired
    private IOrderMemberUserService orderMemberUserService;

    /**
     * 查询会员用户列表
     */
    @PreAuthorize("@ss.hasPermi('member:member:list')")
    @GetMapping("/list")
    public TableDataInfo list(OrderMemberUser orderMemberUser)
    {
        startPage();
        List<OrderMemberUser> list = orderMemberUserService.selectOrderMemberUserList(orderMemberUser);
        return getDataTable(list);
    }

    /**
     * 导出会员用户列表
     */
    @PreAuthorize("@ss.hasPermi('member:member:export')")
    @Log(title = "会员用户", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, OrderMemberUser orderMemberUser)
    {
        List<OrderMemberUser> list = orderMemberUserService.selectOrderMemberUserList(orderMemberUser);
        ExcelUtil<OrderMemberUser> util = new ExcelUtil<OrderMemberUser>(OrderMemberUser.class);
        util.exportExcel(response, list, "会员用户数据");
    }

    /**
     * 获取会员用户详细信息
     */
    @PreAuthorize("@ss.hasPermi('member:member:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(orderMemberUserService.selectOrderMemberUserById(id));
    }

    /**
     * 新增会员用户
     */
    @PreAuthorize("@ss.hasPermi('member:member:add')")
    @Log(title = "会员用户", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody OrderMemberUser orderMemberUser)
    {
        OrderMemberUser byUsername = orderMemberUserService.findByUsername(orderMemberUser.getUsername());
        if (StringUtils.isNotNull(byUsername)){
            return error("用户名重复，请重新输入");
        }
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        orderMemberUser.setPassword(encoder.encode(orderMemberUser.getPassword()));
        orderMemberUser.setTradePassword(encoder.encode(orderMemberUser.getTradePassword()));
        return toAjax(orderMemberUserService.insertOrderMemberUser(orderMemberUser));
    }

    /**
     * 修改会员用户
     */
    @PreAuthorize("@ss.hasPermi('member:member:edit')")
    @Log(title = "会员用户", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody OrderMemberUser orderMemberUser)
    {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        orderMemberUser.setPassword(encoder.encode(orderMemberUser.getPassword()));
        orderMemberUser.setTradePassword(encoder.encode(orderMemberUser.getTradePassword()));
        return toAjax(orderMemberUserService.updateOrderMemberUser(orderMemberUser));
    }

    /**
     * 删除会员用户
     */
    @PreAuthorize("@ss.hasPermi('member:member:remove')")
    @Log(title = "会员用户", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(orderMemberUserService.deleteOrderMemberUserByIds(ids));
    }
}
