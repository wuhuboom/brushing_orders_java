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
import com.order.member.domain.GoodsMemberLevel;
import com.order.member.service.IGoodsMemberLevelService;
import com.order.common.utils.poi.ExcelUtil;
import com.order.common.core.page.TableDataInfo;

/**
 * 等级Controller
 * 
 * @author order
 * @date 2025-10-11
 */
@RestController
@RequestMapping("/member/level")
public class GoodsMemberLevelController extends BaseController
{
    @Autowired
    private IGoodsMemberLevelService goodsMemberLevelService;

    /**
     * 查询等级列表
     */
    @PreAuthorize("@ss.hasPermi('member:level:list')")
    @GetMapping("/list")
    public TableDataInfo list(GoodsMemberLevel goodsMemberLevel)
    {
        startPage();
        List<GoodsMemberLevel> list = goodsMemberLevelService.selectGoodsMemberLevelList(goodsMemberLevel);
        return getDataTable(list);
    }

    /**
     * 导出等级列表
     */
    @PreAuthorize("@ss.hasPermi('member:level:export')")
    @Log(title = "等级", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, GoodsMemberLevel goodsMemberLevel)
    {
        List<GoodsMemberLevel> list = goodsMemberLevelService.selectGoodsMemberLevelList(goodsMemberLevel);
        ExcelUtil<GoodsMemberLevel> util = new ExcelUtil<GoodsMemberLevel>(GoodsMemberLevel.class);
        util.exportExcel(response, list, "等级数据");
    }

    /**
     * 获取等级详细信息
     */
    @PreAuthorize("@ss.hasPermi('member:level:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(goodsMemberLevelService.selectGoodsMemberLevelById(id));
    }

    /**
     * 新增等级
     */
    @PreAuthorize("@ss.hasPermi('member:level:add')")
    @Log(title = "等级", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody GoodsMemberLevel goodsMemberLevel)
    {
        return toAjax(goodsMemberLevelService.insertGoodsMemberLevel(goodsMemberLevel));
    }

    /**
     * 修改等级
     */
    @PreAuthorize("@ss.hasPermi('member:level:edit')")
    @Log(title = "等级", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody GoodsMemberLevel goodsMemberLevel)
    {
        return toAjax(goodsMemberLevelService.updateGoodsMemberLevel(goodsMemberLevel));
    }

    /**
     * 删除等级
     */
    @PreAuthorize("@ss.hasPermi('member:level:remove')")
    @Log(title = "等级", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(goodsMemberLevelService.deleteGoodsMemberLevelByIds(ids));
    }
}
