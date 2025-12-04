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
import com.order.member.domain.OrderLoginLog;
import com.order.member.service.IOrderLoginLogService;
import com.order.common.utils.poi.ExcelUtil;
import com.order.common.core.page.TableDataInfo;

/**
 * 登录日志Controller
 * 
 * @author order
 * @date 2025-11-10
 */
@RestController
@RequestMapping("/member/memberloginlog")
public class OrderLoginLogController extends BaseController
{
    @Autowired
    private IOrderLoginLogService orderLoginLogService;

    /**
     * 查询登录日志列表
     */
    @PreAuthorize("@ss.hasPermi('member:memberloginlog:list')")
    @GetMapping("/list")
    public TableDataInfo list(OrderLoginLog orderLoginLog)
    {
        startPage();
        List<OrderLoginLog> list = orderLoginLogService.selectOrderLoginLogList(orderLoginLog);
        return getDataTable(list);
    }

    /**
     * 导出登录日志列表
     */
    @PreAuthorize("@ss.hasPermi('member:memberloginlog:export')")
    @Log(title = "登录日志", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, OrderLoginLog orderLoginLog)
    {
        List<OrderLoginLog> list = orderLoginLogService.selectOrderLoginLogList(orderLoginLog);
        ExcelUtil<OrderLoginLog> util = new ExcelUtil<OrderLoginLog>(OrderLoginLog.class);
        util.exportExcel(response, list, "登录日志数据");
    }

    /**
     * 获取登录日志详细信息
     */
    @PreAuthorize("@ss.hasPermi('member:memberloginlog:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(orderLoginLogService.selectOrderLoginLogById(id));
    }



    /**
     * 删除登录日志
     */
    @PreAuthorize("@ss.hasPermi('member:memberloginlog:remove')")
    @Log(title = "登录日志", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(orderLoginLogService.deleteOrderLoginLogByIds(ids));
    }
}
