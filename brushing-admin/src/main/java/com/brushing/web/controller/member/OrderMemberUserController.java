package com.brushing.web.controller.member;

import java.math.BigDecimal;
import java.util.List;

import com.alibaba.fastjson2.JSONObject;
import com.brushing.common.utils.SecurityUtils;
import com.brushing.common.utils.StringUtils;
import com.brushing.member.domain.OrderMemberLevel;
import com.brushing.member.service.IOrderMemberLevelService;
import com.brushing.member.service.IOrderTopupService;
import com.brushing.web.controller.member.dto.ScopeUser;
import com.brushing.web.controller.member.dto.TopupDto;
import com.brushing.web.controller.websocket.WebSocketServer;
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

    @Autowired
    private IOrderMemberLevelService orderMemberLevelService;

    @Autowired
    private IOrderTopupService orderTopupService;

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

    @GetMapping("/scopeList")
    public TableDataInfo scopeList(ScopeUser user)
    {
        startPage();
        List<OrderMemberUser> list = orderMemberUserService.selectMembersByScope(user.getUserId(),user.getScope());
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
        OrderMemberUser orderMemberUser = orderMemberUserService.selectOrderMemberUserById(id);
        return success(orderMemberUser);
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
        if(StringUtils.isNotEmpty(orderMemberUser.getTradePassword())){
            orderMemberUser.setTradePassword(encoder.encode(orderMemberUser.getTradePassword()));
        }
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
        OrderMemberUser orderMemberUser2 = orderMemberUserService.selectOrderMemberUserById(orderMemberUser.getId());
        orderMemberUser.setVersion(orderMemberUser2.getVersion());
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (StringUtils.isNotEmpty(orderMemberUser.getPassword())){
            orderMemberUser.setPassword(encoder.encode(orderMemberUser.getPassword()));
        }
        if(StringUtils.isNotEmpty(orderMemberUser.getTradePassword())){
            orderMemberUser.setTradePassword(encoder.encode(orderMemberUser.getTradePassword()));
        }
        if(StringUtils.isNotNull(orderMemberUser.getParentId())){
            if (!orderMemberUser.getParentId().equals(0L)) {
                OrderMemberUser orderMemberUser1 = orderMemberUserService.selectOrderMemberUserById(orderMemberUser.getParentId());
                if (StringUtils.isNull(orderMemberUser1)){
                    return error("上级ID错误");
                }
            }
        }
        int i = orderMemberUserService.updateOrderMemberUser(orderMemberUser);
        JSONObject jsonObject= new JSONObject();
        jsonObject.put("type","1");
        jsonObject.put("data",orderMemberUser);
        WebSocketServer.sendMessageToUser(orderMemberUser.getId(),jsonObject.toJSONString());
        return toAjax(i);
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

    /**
     * 获取等级列表
     * @return
     */
    @GetMapping("/levelList")
    public AjaxResult levelList()
    {
        List<OrderMemberLevel> list = orderMemberLevelService.selectOrderMemberLevelList(new OrderMemberLevel());
        return success(list);
    }

    /**
     * 上下分操作
     * @param dto
     * @return
     */
    @PostMapping("/topupAmount")
    public AjaxResult topupAmount(@RequestBody TopupDto dto){
        int i = orderTopupService.upOrDown(dto.getUserId(), new BigDecimal(dto.getAmount()), dto.getType());
        sendUserInfoMessage(dto.getUserId(),"1");
        return toAjax(i);
    }

    @PostMapping("/upAmount")
    public AjaxResult upAmount(@RequestBody TopupDto dto){
        int i = orderTopupService.uPamount(dto.getUserId(), new BigDecimal(dto.getAmount()), getUserId(), getUsername());
        sendUserInfoMessage(dto.getUserId(),"1");
        return toAjax(i);
    }

    @PostMapping("/updateAmount")
    public AjaxResult updateAmount(@RequestBody TopupDto dto){
        int i = orderTopupService.updateAmount(dto.getUserId(), new BigDecimal(dto.getAmount()), getUserId(), getUsername());
        sendUserInfoMessage(dto.getUserId(),"1");
        return toAjax(i);
    }

    /**
     * 重置单数
     * @return
     */
    @GetMapping("/restDealCount/{id}")
    public AjaxResult restDealCount(@PathVariable("id") Long userId){
        OrderMemberUser orderMemberUser = orderMemberUserService.selectOrderMemberUserById(userId);
        if (orderMemberUser.getDealCount() < orderMemberUser.getUserLevel().getOrderCount() ){
            return error("未达到重置条件");
        }
        orderMemberUser.setDealCount(0);
        orderMemberUser.setTotalResetCount(orderMemberUser.getTotalResetCount()+1);
        orderMemberUser.setTodayResetCount(orderMemberUser.getTodayResetCount()+1);
        int i = orderMemberUserService.updateOrderMemberUser(orderMemberUser);
        sendUserInfoMessage(userId,"1");
        return toAjax(i);
    }


    public void sendUserInfoMessage(Long userId, String type) {
        // 获取用户信息
        OrderMemberUser orderMemberUser = orderMemberUserService.selectOrderMemberUserById(userId);

        if (orderMemberUser != null) {
            // 创建 JSON 对象，构建消息
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", type);  // 可以根据传入的 type 动态改变消息类型
            jsonObject.put("data", orderMemberUser);  // 包含用户信息

            // 发送消息
            WebSocketServer.sendMessageToUser(orderMemberUser.getId(), jsonObject.toJSONString());
        } else {
            System.out.println("User with ID " + userId + " not found.");
        }
    }

    /**
     * 获取统计信息
     * @return
     */
    @GetMapping("/getDashboardData")
    public AjaxResult getDashboardData(){

        return success(orderMemberUserService.getDashboardData());
    }
}
