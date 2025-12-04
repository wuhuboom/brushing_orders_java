package com.order.web.controller.member;

import java.util.List;

import com.order.member.domain.OrderWithdrawalType;
import com.order.member.service.IOrderWithdrawalTypeService;
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
import com.order.member.domain.GoodsWithdrawalAccount;
import com.order.member.service.IGoodsWithdrawalAccountService;
import com.order.common.utils.poi.ExcelUtil;
import com.order.common.core.page.TableDataInfo;

/**
 * 提现账户Controller
 * 
 * @author order
 * @date 2025-11-05
 */
@RestController
@RequestMapping("/member/withdrawalAcc")
public class GoodsWithdrawalAccountController extends BaseController
{
    @Autowired
    private IGoodsWithdrawalAccountService goodsWithdrawalAccountService;

    @Autowired
    private IOrderWithdrawalTypeService orderWithdrawalTypeService;

    /**
     * 查询提现账户列表
     */
    @PreAuthorize("@ss.hasPermi('member:withdrawalAcc:list')")
    @GetMapping("/list")
    public TableDataInfo list(GoodsWithdrawalAccount goodsWithdrawalAccount)
    {
        startPage();
        List<GoodsWithdrawalAccount> list = goodsWithdrawalAccountService.selectGoodsWithdrawalAccountList(goodsWithdrawalAccount);
        return getDataTable(list);
    }

    /**
     * 导出提现账户列表
     */
    @PreAuthorize("@ss.hasPermi('member:withdrawalAcc:export')")
    @Log(title = "提现账户", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, GoodsWithdrawalAccount goodsWithdrawalAccount)
    {
        List<GoodsWithdrawalAccount> list = goodsWithdrawalAccountService.selectGoodsWithdrawalAccountList(goodsWithdrawalAccount);
        ExcelUtil<GoodsWithdrawalAccount> util = new ExcelUtil<GoodsWithdrawalAccount>(GoodsWithdrawalAccount.class);
        util.exportExcel(response, list, "提现账户数据");
    }

    /**
     * 获取提现账户详细信息
     */
    @PreAuthorize("@ss.hasPermi('member:withdrawalAcc:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(goodsWithdrawalAccountService.selectGoodsWithdrawalAccountById(id));
    }

    /**
     * 新增提现账户
     */
    @PreAuthorize("@ss.hasPermi('member:withdrawalAcc:add')")
    @Log(title = "提现账户", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody GoodsWithdrawalAccount goodsWithdrawalAccount)
    {
        return toAjax(goodsWithdrawalAccountService.insertGoodsWithdrawalAccount(goodsWithdrawalAccount));
    }

    @GetMapping("/getType")
    public AjaxResult getType(OrderWithdrawalType orderWithdrawalType){
        List<OrderWithdrawalType> orderWithdrawalTypes = orderWithdrawalTypeService.selectOrderWithdrawalTypeList(orderWithdrawalType);
        return AjaxResult.success(orderWithdrawalTypes);
    }

    /**
     * 修改提现账户
     */
    @PreAuthorize("@ss.hasPermi('member:withdrawalAcc:edit')")
    @Log(title = "提现账户", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody GoodsWithdrawalAccount goodsWithdrawalAccount)
    {
        return toAjax(goodsWithdrawalAccountService.updateGoodsWithdrawalAccount(goodsWithdrawalAccount));
    }

    /**
     * 删除提现账户
     */
    @PreAuthorize("@ss.hasPermi('member:withdrawalAcc:remove')")
    @Log(title = "提现账户", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(goodsWithdrawalAccountService.deleteGoodsWithdrawalAccountByIds(ids));
    }
}
