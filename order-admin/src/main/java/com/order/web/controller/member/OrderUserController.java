package com.order.web.controller.member;

import java.math.BigDecimal;
import java.util.List;

import com.order.common.utils.StringUtils;
import com.order.member.domain.GoodsMemberLevel;
import com.order.member.domain.dto.TransactionFlowResult;
import com.order.member.service.IGoodsMemberLevelService;
import com.order.member.service.ITransactionService;
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
import com.order.common.annotation.Log;
import com.order.common.core.controller.BaseController;
import com.order.common.core.domain.AjaxResult;
import com.order.common.enums.BusinessType;
import com.order.member.domain.OrderUser;
import com.order.member.service.IOrderUserService;
import com.order.common.utils.poi.ExcelUtil;
import com.order.common.core.page.TableDataInfo;
import com.order.member.domain.dto.TransactionDto;

/**
 * 订单用户Controller
 * 
 * @author order
 * @date 2025-10-21
 */
@RestController
@RequestMapping("/member/orderuser")
public class OrderUserController extends BaseController
{
    @Autowired
    private IOrderUserService orderUserService;

    @Autowired
    private IGoodsMemberLevelService goodsMemberLevelService;

    @Autowired
    private ITransactionService transactionService;

    /**
     * 查询订单用户列表
     */
    @PreAuthorize("@ss.hasPermi('member:orderuser:list')")
    @GetMapping("/list")
    public TableDataInfo list(OrderUser orderUser)
    {
        startPage();
        List<OrderUser> list = orderUserService.selectOrderUserList(orderUser);
        return getDataTable(list);
    }

    @GetMapping("/getLevel")
    public AjaxResult getLevel(){
        List<GoodsMemberLevel> list = goodsMemberLevelService.selectGoodsMemberLevelList(null);
        return success(list);
    }

    @GetMapping("/allUser")
    public AjaxResult getAllUser(){
        List<OrderUser> list = orderUserService.selectOrderUserList(null);
        return success(list);
    }

    /**
     * 导出订单用户列表
     */
    @PreAuthorize("@ss.hasPermi('member:orderuser:export')")
    @Log(title = "订单用户", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, OrderUser orderUser)
    {
        List<OrderUser> list = orderUserService.selectOrderUserList(orderUser);
        ExcelUtil<OrderUser> util = new ExcelUtil<OrderUser>(OrderUser.class);
        util.exportExcel(response, list, "订单用户数据");
    }

    /**
     * 获取订单用户详细信息
     */
    @PreAuthorize("@ss.hasPermi('member:orderuser:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(orderUserService.selectOrderUserById(id));
    }

    /**
     * 新增订单用户
     */
    @PreAuthorize("@ss.hasPermi('member:orderuser:add')")
    @Log(title = "订单用户", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody OrderUser orderUser)
    {
        OrderUser orderUser1 = orderUserService.selectOrderUserByName(orderUser.getUsername());
        if (StringUtils.isNotNull(orderUser1)){
            return AjaxResult.error("用户名重复");
        }

        Long parentId = orderUser.getParentId();
        if (!parentId.equals(0L))
        {
            OrderUser orderUser2 = orderUserService.selectOrderUserById(parentId);
            if (StringUtils.isNull(orderUser2)){
                return AjaxResult.error("上级用户不存在");
            }
        }
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        orderUser.setPassword(encoder.encode(orderUser.getPassword()));
        orderUser.setTradePassword(encoder.encode(orderUser.getTradePassword()));
        return toAjax(orderUserService.insertOrderUser(orderUser));
    }

    /**
     * 修改订单用户
     */
    @PreAuthorize("@ss.hasPermi('member:orderuser:edit')")
    @Log(title = "订单用户", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody OrderUser orderUser)
    {
        OrderUser orderUser1 = orderUserService.selectOrderUserById(orderUser.getId());
        orderUser.setVersion(orderUser1.getVersion());
        orderUser.setTradePassword(null);
        orderUser.setPassword(null);
        orderUser.setUsername(null);
        return toAjax(orderUserService.updateOrderUser(orderUser));
    }

    //修改密码
    @PutMapping("/editPassword")
    public AjaxResult editPassword(@RequestBody OrderUser orderUser)
    {
        OrderUser orderUser1 = orderUserService.selectOrderUserById(orderUser.getId());
        orderUser.setVersion(orderUser1.getVersion());
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        orderUser.setPassword(encoder.encode(orderUser.getPassword()));
        return toAjax(orderUserService.updateOrderUser(orderUser));
    }

    //修改交易密码
    @PutMapping("/editTradePassword")
    public AjaxResult editTradePassword(@RequestBody OrderUser orderUser)
    {
        OrderUser orderUser1 = orderUserService.selectOrderUserById(orderUser.getId());
        orderUser.setVersion(orderUser1.getVersion());
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        orderUser.setTradePassword(encoder.encode(orderUser.getTradePassword()));
        return toAjax(orderUserService.updateOrderUser(orderUser));
    }

    @PutMapping("/editParentId")
    public AjaxResult editParentId(@RequestBody OrderUser orderUser)
    {
        OrderUser old = orderUserService.selectOrderUserById(orderUser.getId());
        if (orderUser.getId().equals(old.getParentId())){
            return error("不能设置自己为上级用户");
        }
        if (orderUser.getParentId().equals(old.getParentId())){
            return error("上级用户未修改");
        }
        if (orderUser.getParentId().equals(0L) ){
            return toAjax(  orderUserService.updateParentIdAndAncestors(old.getId(), 0L, old.getVersion()));
        }
        OrderUser parent = orderUserService.selectOrderUserById(orderUser.getParentId());
        if (StringUtils.isNull(parent)) {
            return error("上级用户不存在");
        }
        return toAjax( orderUserService.updateParentIdAndAncestors(old.getId(), parent.getId(), old.getVersion()));
    }

    /**
     * 删除订单用户
     */
    @PreAuthorize("@ss.hasPermi('member:orderuser:remove')")
    @Log(title = "订单用户", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(orderUserService.deleteOrderUserByIds(ids));
    }

    @Log(title = "订单用户交易", businessType = BusinessType.OTHER)
    @PostMapping("/transaction")
    public AjaxResult transaction(@RequestBody TransactionDto dto)
    {
        // 简单校验
        if (dto == null) {
            return AjaxResult.error("请求体不能为空");
        }
        if (dto.getAmount() == null) {
            return AjaxResult.error("金额不能为空");
        }
        if (dto.getUserId() == null) {
            return AjaxResult.error("userId 不能为空");
        }
        try {
            // 始终通过服务的统一入口处理，服务内部会根据 operationType 调用 recordRecharge 或 recordFlow 等通用方法
            boolean ok = transactionService.processTransaction(dto);
            if (ok) {
                return AjaxResult.success("处理成功");
            } else {
                return AjaxResult.error("处理失败");
            }
        } catch (Exception ex) {
            return AjaxResult.error(ex.getMessage());
        }
    }


    @GetMapping("/resetOrder/{id}")
    public AjaxResult resetOrder(@PathVariable("id") Long id){
        OrderUser orderUser = orderUserService.selectOrderUserById(id);
        GoodsMemberLevel memberLevel = orderUser.getMemberLevel();
        if (!orderUser.getTaskProgress().equals(memberLevel.getOrderCountPerDay())){
            return AjaxResult.error("需要完成全部任务");
        }
        orderUser.setTaskProgress(0L);
        orderUser.setTodayRest(orderUser.getTodayRest()+1);
        orderUser.setTotalRest(orderUser.getTotalRest()+1);
        orderUserService.updateOrderUser(orderUser);
        return AjaxResult.success("重置成功");
    }

    @PostMapping("/giftAmount")
    public AjaxResult giftAmount(@RequestBody TransactionDto dto) {
        OrderUser orderUser = orderUserService.selectOrderUserById(dto.getUserId());
        if (orderUser == null) {
            return AjaxResult.error("用户不存在");
        }
        BigDecimal amount = dto.getAmount();
        try {
            TransactionFlowResult result = transactionService.recordFlow(dto.getUserId(),"zs", dto.getAmount(),orderUser.getBalance(), dto.getRemark());
            BigDecimal add = orderUser.getBalance().add(dto.getAmount());
            orderUser.setBalance(add);
            orderUserService.updateOrderUser(orderUser);
            return AjaxResult.success("赠送成功");
        } catch (Exception ex) {
            return AjaxResult.error(ex.getMessage());
        }
    }


    @GetMapping("/selectChildrenById")
    public TableDataInfo selectChildrenById(OrderUser orderUser)
    {
        startPage();
        List<OrderUser> list = orderUserService.selectChildrenById(orderUser.getId(), orderUser.getScope());
        return getDataTable(list);
    }


}
