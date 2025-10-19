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
import com.brushing.member.domain.OrderBankWallet;
import com.brushing.member.service.IOrderBankWalletService;
import com.brushing.common.utils.poi.ExcelUtil;
import com.brushing.common.core.page.TableDataInfo;

/**
 * 银行钱包Controller
 * 
 * @author brushing
 * @date 2025-10-13
 */
@RestController
@RequestMapping("/member/wallet")
public class OrderBankWalletController extends BaseController
{
    @Autowired
    private IOrderBankWalletService orderBankWalletService;

    /**
     * 查询银行钱包列表
     */
    @GetMapping("/list")
    public TableDataInfo list(OrderBankWallet orderBankWallet)
    {
        startPage();
        List<OrderBankWallet> list = orderBankWalletService.selectOrderBankWalletList(orderBankWallet);
        return getDataTable(list);
    }

    @GetMapping("/userBankList/{userId}")
    public AjaxResult userBankList(@PathVariable("userId") Long userId){
        OrderBankWallet orderBankWallet = new OrderBankWallet();
        orderBankWallet.setUserId(userId);
        List<OrderBankWallet> list = orderBankWalletService.selectOrderBankWalletList(orderBankWallet);
        return success(list);
    }

    /**
     * 导出银行钱包列表
     */
    @Log(title = "银行钱包", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, OrderBankWallet orderBankWallet)
    {
        List<OrderBankWallet> list = orderBankWalletService.selectOrderBankWalletList(orderBankWallet);
        ExcelUtil<OrderBankWallet> util = new ExcelUtil<OrderBankWallet>(OrderBankWallet.class);
        util.exportExcel(response, list, "银行钱包数据");
    }

    /**
     * 获取银行钱包详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(orderBankWalletService.selectOrderBankWalletById(id));
    }

    /**
     * 新增银行钱包
     */
    @Log(title = "银行钱包", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody OrderBankWallet orderBankWallet)
    {
        return toAjax(orderBankWalletService.insertOrderBankWallet(orderBankWallet));
    }

    /**
     * 修改银行钱包
     */
    @Log(title = "银行钱包", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody OrderBankWallet orderBankWallet)
    {
        return toAjax(orderBankWalletService.updateOrderBankWallet(orderBankWallet));
    }

    /**
     * 删除银行钱包
     */
    @Log(title = "银行钱包", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(orderBankWalletService.deleteOrderBankWalletByIds(ids));
    }
}
