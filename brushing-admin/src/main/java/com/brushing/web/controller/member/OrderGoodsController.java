package com.brushing.web.controller.member;

import java.util.List;

import com.brushing.member.domain.OrderGoodsType;
import com.brushing.member.service.IOrderGoodsTypeService;
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
import com.brushing.member.domain.OrderGoods;
import com.brushing.member.service.IOrderGoodsService;
import com.brushing.common.utils.poi.ExcelUtil;
import com.brushing.common.core.page.TableDataInfo;
import com.brushing.common.utils.DateUtils;
import com.brushing.set.service.IOrderSiteConfigService;
import com.brushing.set.domain.OrderSiteConfig;
import com.brushing.member.mapper.OrderGoodsHotelMapper;

/**
 * 商品列表Controller
 * 
 * @author brushing
 * @date 2025-08-01
 */
@RestController
@RequestMapping("/member/goods")
public class OrderGoodsController extends BaseController
{
    @Autowired
    private IOrderGoodsService orderGoodsService;

    @Autowired
    private IOrderGoodsTypeService orderGoodsTypeService;

    @Autowired
    private IOrderSiteConfigService siteConfigService;

    @Autowired
    private OrderGoodsHotelMapper orderGoodsHotelMapper;

    /**
     * 查询商品列表列表
     */
    @PreAuthorize("@ss.hasPermi('member:goods:list')")
    @GetMapping("/list")
    public TableDataInfo list(OrderGoods orderGoods)
    {
        OrderSiteConfig siteConfig = siteConfigService.selectOrderSiteConfigById(1L);
        if (siteConfig != null && "2".equals(siteConfig.getGoodsTableType())){
            startPage();
            List<OrderGoods> list = orderGoodsHotelMapper.selectOrderGoodsList(orderGoods);
            return getDataTable(list);
        }else{
            startPage();
            List<OrderGoods> list = orderGoodsService.selectOrderGoodsList(orderGoods);
            return getDataTable(list);
        }
    }

    @GetMapping("/getTypeList")
    public AjaxResult getTypeList(){
        List<OrderGoodsType> orderGoodsTypes = orderGoodsTypeService.selectOrderGoodsTypeList(null);
        return success(orderGoodsTypes);
    }

    /**
     * 导出商品列表列表
     */
    @PreAuthorize("@ss.hasPermi('member:goods:export')")
    @Log(title = "商品列表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, OrderGoods orderGoods)
    {
        OrderSiteConfig siteConfig = siteConfigService.selectOrderSiteConfigById(1L);
        List<OrderGoods> list;
        if (siteConfig != null && "2".equals(siteConfig.getGoodsTableType())){
            list = orderGoodsHotelMapper.selectOrderGoodsList(orderGoods);
        }else{
            list = orderGoodsService.selectOrderGoodsList(orderGoods);
        }
        ExcelUtil<OrderGoods> util = new ExcelUtil<OrderGoods>(OrderGoods.class);
        util.exportExcel(response, list, "商品列表数据");
    }

    /**
     * 获取商品列表详细信息
     */
    @PreAuthorize("@ss.hasPermi('member:goods:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        OrderSiteConfig siteConfig = siteConfigService.selectOrderSiteConfigById(1L);
        if (siteConfig != null && "2".equals(siteConfig.getGoodsTableType())){
            return success(orderGoodsHotelMapper.selectOrderGoodsById(id));
        }
        return success(orderGoodsService.selectOrderGoodsById(id));
    }

    /**
     * 新增商品列表
     */
    @PreAuthorize("@ss.hasPermi('member:goods:add')")
    @Log(title = "商品列表", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody OrderGoods orderGoods)
    {
        OrderSiteConfig siteConfig = siteConfigService.selectOrderSiteConfigById(1L);
        if (siteConfig != null && "2".equals(siteConfig.getGoodsTableType())){
            orderGoods.setCreateTime(DateUtils.getNowDate());
            int r = orderGoodsHotelMapper.insertOrderGoods(orderGoods);
            return toAjax(r);
        }
        return toAjax(orderGoodsService.insertOrderGoods(orderGoods));
    }

    /**
     * 修改商品列表
     */
    @PreAuthorize("@ss.hasPermi('member:goods:edit')")
    @Log(title = "商品列表", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody OrderGoods orderGoods)
    {
        OrderSiteConfig siteConfig = siteConfigService.selectOrderSiteConfigById(1L);
        if (siteConfig != null && "2".equals(siteConfig.getGoodsTableType())){
            orderGoods.setUpdateTime(DateUtils.getNowDate());
            orderGoodsHotelMapper.updateOrderGoods(orderGoods);
            return toAjax(1);
        }
        return toAjax(orderGoodsService.updateOrderGoods(orderGoods));
    }

    /**
     * 删除商品列表
     */
    @PreAuthorize("@ss.hasPermi('member:goods:remove')")
    @Log(title = "商品列表", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        OrderSiteConfig siteConfig = siteConfigService.selectOrderSiteConfigById(1L);
        if (siteConfig != null && "2".equals(siteConfig.getGoodsTableType())){
            orderGoodsHotelMapper.deleteOrderGoodsByIds(ids);
            return toAjax(1);
        }
        return toAjax(orderGoodsService.deleteOrderGoodsByIds(ids));
    }
}
