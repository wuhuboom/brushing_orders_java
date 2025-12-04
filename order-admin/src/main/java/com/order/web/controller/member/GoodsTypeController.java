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
import com.order.member.domain.GoodsType;
import com.order.member.service.IGoodsTypeService;
import com.order.common.utils.poi.ExcelUtil;
import com.order.common.core.page.TableDataInfo;

/**
 * 类目管理Controller
 * 
 * @author order
 * @date 2025-10-11
 */
@RestController
@RequestMapping("/member/goodstype")
public class GoodsTypeController extends BaseController
{
    @Autowired
    private IGoodsTypeService goodsTypeService;

    /**
     * 查询类目管理列表
     */
    @PreAuthorize("@ss.hasPermi('member:goodstype:list')")
    @GetMapping("/list")
    public TableDataInfo list(GoodsType goodsType)
    {
        startPage();
        List<GoodsType> list = goodsTypeService.selectGoodsTypeList(goodsType);
        return getDataTable(list);
    }

    /**
     * 导出类目管理列表
     */
    @PreAuthorize("@ss.hasPermi('member:goodstype:export')")
    @Log(title = "类目管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, GoodsType goodsType)
    {
        List<GoodsType> list = goodsTypeService.selectGoodsTypeList(goodsType);
        ExcelUtil<GoodsType> util = new ExcelUtil<GoodsType>(GoodsType.class);
        util.exportExcel(response, list, "类目管理数据");
    }

    /**
     * 获取类目管理详细信息
     */
    @PreAuthorize("@ss.hasPermi('member:goodstype:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(goodsTypeService.selectGoodsTypeById(id));
    }

    /**
     * 新增类目管理
     */
    @PreAuthorize("@ss.hasPermi('member:goodstype:add')")
    @Log(title = "类目管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody GoodsType goodsType)
    {
        return toAjax(goodsTypeService.insertGoodsType(goodsType));
    }

    /**
     * 修改类目管理
     */
    @PreAuthorize("@ss.hasPermi('member:goodstype:edit')")
    @Log(title = "类目管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody GoodsType goodsType)
    {
        return toAjax(goodsTypeService.updateGoodsType(goodsType));
    }

    /**
     * 删除类目管理
     */
    @PreAuthorize("@ss.hasPermi('member:goodstype:remove')")
    @Log(title = "类目管理", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(goodsTypeService.deleteGoodsTypeByIds(ids));
    }
}
