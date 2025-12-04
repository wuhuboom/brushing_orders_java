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
import com.order.member.domain.GoodsBanner;
import com.order.member.service.IGoodsBannerService;
import com.order.common.utils.poi.ExcelUtil;
import com.order.common.core.page.TableDataInfo;

/**
 * 横幅：用于存储横幅广告相关信息Controller
 * 
 * @author order
 * @date 2025-11-11
 */
@RestController
@RequestMapping("/member/banner")
public class GoodsBannerController extends BaseController
{
    @Autowired
    private IGoodsBannerService goodsBannerService;

    /**
     * 查询横幅：用于存储横幅广告相关信息列表
     */
    @PreAuthorize("@ss.hasPermi('member:banner:list')")
    @GetMapping("/list")
    public TableDataInfo list(GoodsBanner goodsBanner)
    {
        startPage();
        List<GoodsBanner> list = goodsBannerService.selectGoodsBannerList(goodsBanner);
        return getDataTable(list);
    }

    /**
     * 导出横幅：用于存储横幅广告相关信息列表
     */
    @PreAuthorize("@ss.hasPermi('member:banner:export')")
    @Log(title = "横幅：用于存储横幅广告相关信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, GoodsBanner goodsBanner)
    {
        List<GoodsBanner> list = goodsBannerService.selectGoodsBannerList(goodsBanner);
        ExcelUtil<GoodsBanner> util = new ExcelUtil<GoodsBanner>(GoodsBanner.class);
        util.exportExcel(response, list, "横幅：用于存储横幅广告相关信息数据");
    }

    /**
     * 获取横幅：用于存储横幅广告相关信息详细信息
     */
    @PreAuthorize("@ss.hasPermi('member:banner:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(goodsBannerService.selectGoodsBannerById(id));
    }

    /**
     * 新增横幅：用于存储横幅广告相关信息
     */
    @PreAuthorize("@ss.hasPermi('member:banner:add')")
    @Log(title = "横幅：用于存储横幅广告相关信息", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody GoodsBanner goodsBanner)
    {
        return toAjax(goodsBannerService.insertGoodsBanner(goodsBanner));
    }

    /**
     * 修改横幅：用于存储横幅广告相关信息
     */
    @PreAuthorize("@ss.hasPermi('member:banner:edit')")
    @Log(title = "横幅：用于存储横幅广告相关信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody GoodsBanner goodsBanner)
    {
        return toAjax(goodsBannerService.updateGoodsBanner(goodsBanner));
    }

    /**
     * 删除横幅：用于存储横幅广告相关信息
     */
    @PreAuthorize("@ss.hasPermi('member:banner:remove')")
    @Log(title = "横幅：用于存储横幅广告相关信息", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(goodsBannerService.deleteGoodsBannerByIds(ids));
    }
}
