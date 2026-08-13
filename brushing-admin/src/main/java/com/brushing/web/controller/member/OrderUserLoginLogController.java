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
import com.brushing.member.domain.OrderUserLoginLog;
import com.brushing.member.service.IOrderUserLoginLogService;
import com.brushing.common.utils.poi.ExcelUtil;
import com.brushing.common.core.page.TableDataInfo;

/**
 * 会员登录日志Controller
 *
 * @author brushing
 * @date 2026-01-11
 */
@RestController
@RequestMapping("/member/memberlog")
public class OrderUserLoginLogController extends BaseController
{
    @Autowired
    private IOrderUserLoginLogService orderUserLoginLogService;

    /**
     * 查询会员登录日志列表
     */
    @PreAuthorize("@ss.hasPermi('member:memberlog:list')")
    @GetMapping("/list")
    public TableDataInfo list(OrderUserLoginLog orderUserLoginLog)
    {
        startPage();
        List<OrderUserLoginLog> list = orderUserLoginLogService.selectOrderUserLoginLogList(orderUserLoginLog);
        return getDataTable(list);
    }

    /**
     * 导出会员登录日志列表
     */
    @PreAuthorize("@ss.hasPermi('member:memberlog:export')")
    @Log(title = "会员登录日志", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, OrderUserLoginLog orderUserLoginLog)
    {
        List<OrderUserLoginLog> list = orderUserLoginLogService.selectOrderUserLoginLogList(orderUserLoginLog);
        ExcelUtil<OrderUserLoginLog> util = new ExcelUtil<OrderUserLoginLog>(OrderUserLoginLog.class);
        util.exportExcel(response, list, "会员登录日志数据");
    }

    /**
     * 获取会员登录日志详细信息
     */
    @PreAuthorize("@ss.hasPermi('member:memberlog:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(orderUserLoginLogService.selectOrderUserLoginLogById(id));
    }

    /**
     * 新增会员登录日志
     */
    @PreAuthorize("@ss.hasPermi('member:memberlog:add')")
    @Log(title = "会员登录日志", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody OrderUserLoginLog orderUserLoginLog)
    {
        return toAjax(orderUserLoginLogService.insertOrderUserLoginLog(orderUserLoginLog));
    }

    /**
     * 修改会员登录日志
     */
    @PreAuthorize("@ss.hasPermi('member:memberlog:edit')")
    @Log(title = "会员登录日志", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody OrderUserLoginLog orderUserLoginLog)
    {
        return toAjax(orderUserLoginLogService.updateOrderUserLoginLog(orderUserLoginLog));
    }

    /**
     * 删除会员登录日志
     */
    @PreAuthorize("@ss.hasPermi('member:memberlog:remove')")
    @Log(title = "会员登录日志", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(orderUserLoginLogService.deleteOrderUserLoginLogByIds(ids));
    }
}
