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
import com.order.common.annotation.Log;
import com.order.common.core.controller.BaseController;
import com.order.common.core.domain.AjaxResult;
import com.order.common.enums.BusinessType;
import com.order.member.domain.GoodsExtraCommissionSetting;
import com.order.member.service.IGoodsExtraCommissionSettingService;
import com.order.common.utils.poi.ExcelUtil;
import com.order.common.core.page.TableDataInfo;

/**
 * 额外佣金设置Controller
 * 
 * @author order
 * @date 2025-11-08
 */
@RestController
@RequestMapping("/member/extracommission")
public class GoodsExtraCommissionSettingController extends BaseController
{
    @Autowired
    private IGoodsExtraCommissionSettingService goodsExtraCommissionSettingService;

    /**
     * 查询额外佣金设置列表
     */
    @PreAuthorize("@ss.hasPermi('member:extracommission:list')")
    @GetMapping("/list")
    public TableDataInfo list(GoodsExtraCommissionSetting goodsExtraCommissionSetting)
    {
        startPage();
        List<GoodsExtraCommissionSetting> list = goodsExtraCommissionSettingService.selectGoodsExtraCommissionSettingList(goodsExtraCommissionSetting);
        return getDataTable(list);
    }

    /**
     * 导出额外佣金设置列表
     */
    @Log(title = "额外佣金设置", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('member:extracommission:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, GoodsExtraCommissionSetting goodsExtraCommissionSetting)
    {
        List<GoodsExtraCommissionSetting> list = goodsExtraCommissionSettingService.selectGoodsExtraCommissionSettingList(goodsExtraCommissionSetting);
        ExcelUtil<GoodsExtraCommissionSetting> util = new ExcelUtil<GoodsExtraCommissionSetting>(GoodsExtraCommissionSetting.class);
        util.exportExcel(response, list, "额外佣金设置数据");
    }

    /**
     * 获取额外佣金设置详细信息
     */
    @PreAuthorize("@ss.hasPermi('member:extracommission:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(goodsExtraCommissionSettingService.selectGoodsExtraCommissionSettingById(id));
    }

    /**
     * 新增额外佣金设置
     */
    @Log(title = "额外佣金设置", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('member:extracommission:add')")
    @PostMapping
    public AjaxResult add(@RequestBody GoodsExtraCommissionSetting goodsExtraCommissionSetting)
    {
        return toAjax(goodsExtraCommissionSettingService.insertGoodsExtraCommissionSetting(goodsExtraCommissionSetting));
    }

    /**
     * 修改额外佣金设置
     */
    @Log(title = "额外佣金设置", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('member:extracommission:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody GoodsExtraCommissionSetting goodsExtraCommissionSetting)
    {
        return toAjax(goodsExtraCommissionSettingService.updateGoodsExtraCommissionSetting(goodsExtraCommissionSetting));
    }

    /**
     * 删除额外佣金设置
     */
    @Log(title = "额外佣金设置", businessType = BusinessType.DELETE)
	@PreAuthorize("@ss.hasPermi('member:extracommission:remove')")
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(goodsExtraCommissionSettingService.deleteGoodsExtraCommissionSettingByIds(ids));
    }
}
