package com.order.web.controller.member;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.order.common.core.controller.BaseController;
import com.order.common.core.domain.AjaxResult;
import com.order.member.domain.OrderLink;
import com.order.member.service.IOrderLinkService;
import com.order.common.utils.poi.ExcelUtil;
import com.order.common.core.page.TableDataInfo;

/**
 * 连单Controller
 *
 * @author order
 * @date 2025-10-29
 */
@RestController
@RequestMapping("/member/orderlink")
public class OrderLinkController extends BaseController
{
    @Autowired
    private IOrderLinkService orderLinkService;

    /**
     * 查询连单列表
     */
    @PreAuthorize("@ss.hasPermi('member:orderlink:list')")
    @GetMapping("/list")
    public TableDataInfo list(OrderLink orderLink)
    {
        startPage();
        List<OrderLink> list = orderLinkService.selectOrderLinkList(orderLink);
        return getDataTable(list);
    }

    /**
     * 导出连单列表
     */
    @PreAuthorize("@ss.hasPermi('member:orderlink:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, OrderLink orderLink)
    {
        List<OrderLink> list = orderLinkService.selectOrderLinkList(orderLink);
        ExcelUtil<OrderLink> util = new ExcelUtil<OrderLink>(OrderLink.class);
        util.exportExcel(response, list, "连单数据");
    }

    /**
     * 获取连单详细信息
     */
    @PreAuthorize("@ss.hasPermi('member:orderlink:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(orderLinkService.selectOrderLinkById(id));
    }

    /**
     * 新增连单
     */
    @PreAuthorize("@ss.hasPermi('member:orderlink:add')")
    @PostMapping
    public AjaxResult add(@RequestBody OrderLink orderLink)
    {
        String username = getUsername();
        orderLink.setCreateBy(username);
        orderLink.setUpdateBy(username);
        return toAjax(orderLinkService.insertOrderLink(orderLink));
    }

    /**
     * 修改连单
     */
    @PreAuthorize("@ss.hasPermi('member:orderlink:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody OrderLink orderLink)
    {
        orderLink.setUpdateBy(getUsername());
        return toAjax(orderLinkService.updateOrderLink(orderLink));
    }

    /**
     * 删除连单
     */
    @PreAuthorize("@ss.hasPermi('member:orderlink:remove')")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(orderLinkService.deleteOrderLinkByIds(ids));
    }
}
