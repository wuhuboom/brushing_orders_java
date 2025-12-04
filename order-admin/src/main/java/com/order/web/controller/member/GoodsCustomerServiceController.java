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
import com.order.member.domain.GoodsCustomerService;
import com.order.member.service.IGoodsCustomerServiceService;
import com.order.common.utils.poi.ExcelUtil;
import com.order.common.core.page.TableDataInfo;

/**
 * 客服Controller
 * 
 * @author order
 * @date 2025-11-11
 */
@RestController
@RequestMapping("/member/cusservice")
public class GoodsCustomerServiceController extends BaseController
{
    @Autowired
    private IGoodsCustomerServiceService goodsCustomerServiceService;

    /**
     * 查询客服列表
     */
    @PreAuthorize("@ss.hasPermi('member:cusservice:list')")
    @GetMapping("/list")
    public TableDataInfo list(GoodsCustomerService goodsCustomerService)
    {
        startPage();
        List<GoodsCustomerService> list = goodsCustomerServiceService.selectGoodsCustomerServiceList(goodsCustomerService);
        return getDataTable(list);
    }

    /**
     * 导出客服列表
     */
    @PreAuthorize("@ss.hasPermi('member:cusservice:export')")
    @Log(title = "客服", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, GoodsCustomerService goodsCustomerService)
    {
        List<GoodsCustomerService> list = goodsCustomerServiceService.selectGoodsCustomerServiceList(goodsCustomerService);
        ExcelUtil<GoodsCustomerService> util = new ExcelUtil<GoodsCustomerService>(GoodsCustomerService.class);
        util.exportExcel(response, list, "客服数据");
    }

    /**
     * 获取客服详细信息
     */
    @PreAuthorize("@ss.hasPermi('member:cusservice:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return success(goodsCustomerServiceService.selectGoodsCustomerServiceById(id));
    }

    /**
     * 新增客服
     */
    @PreAuthorize("@ss.hasPermi('member:cusservice:add')")
    @Log(title = "客服", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody GoodsCustomerService goodsCustomerService)
    {
        return toAjax(goodsCustomerServiceService.insertGoodsCustomerService(goodsCustomerService));
    }

    /**
     * 修改客服
     */
    @PreAuthorize("@ss.hasPermi('member:cusservice:edit')")
    @Log(title = "客服", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody GoodsCustomerService goodsCustomerService)
    {
        return toAjax(goodsCustomerServiceService.updateGoodsCustomerService(goodsCustomerService));
    }

    /**
     * 删除客服
     */
    @PreAuthorize("@ss.hasPermi('member:cusservice:remove')")
    @Log(title = "客服", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(goodsCustomerServiceService.deleteGoodsCustomerServiceByIds(ids));
    }
}
