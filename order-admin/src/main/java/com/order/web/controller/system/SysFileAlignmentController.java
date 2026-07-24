package com.order.web.controller.system;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.order.common.annotation.Log;
import com.order.common.core.controller.BaseController;
import com.order.common.core.domain.AjaxResult;
import com.order.common.core.page.TableDataInfo;
import com.order.common.enums.BusinessType;
import com.order.system.service.ISystemAlignmentService;

@RestController
@RequestMapping("/system/file")
public class SysFileAlignmentController extends BaseController
{
    @Autowired
    private ISystemAlignmentService service;

    @PreAuthorize("@ss.hasAnyPermi('system:file:list,system:config:list')")
    @GetMapping("/files/list")
    public TableDataInfo fileList(@RequestParam Map<String, Object> params)
    {
        startPage();
        return getDataTable(service.selectFileList(params));
    }

    @PreAuthorize("@ss.hasAnyPermi('system:file:query,system:config:query')")
    @GetMapping("/files/{id}")
    public AjaxResult file(@PathVariable Long id)
    {
        return success(service.selectFile(id));
    }

    @Log(title = "文件管理", businessType = BusinessType.DELETE)
    @PreAuthorize("@ss.hasAnyPermi('system:file:remove,system:config:remove')")
    @DeleteMapping("/files/{ids}")
    public AjaxResult removeFiles(@PathVariable Long[] ids)
    {
        return toAjax(service.deleteFiles(ids));
    }

    @PreAuthorize("@ss.hasAnyPermi('system:file-reference:list,system:file:list')")
    @GetMapping("/file-references/list")
    public TableDataInfo referenceList(@RequestParam Map<String, Object> params)
    {
        startPage();
        return getDataTable(service.selectFileReferenceList(params));
    }

    @PreAuthorize("@ss.hasAnyPermi('system:file-reference:query,system:file:query')")
    @GetMapping("/file-references/{id}")
    public AjaxResult reference(@PathVariable Long id)
    {
        return success(service.selectFileReference(id));
    }

    @Log(title = "文件引用", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasAnyPermi('system:file-reference:add,system:file:add')")
    @PostMapping("/file-references")
    public AjaxResult addReference(@RequestBody Map<String, Object> data)
    {
        return toAjax(service.insertFileReference(data));
    }

    @Log(title = "文件引用", businessType = BusinessType.DELETE)
    @PreAuthorize("@ss.hasAnyPermi('system:file-reference:remove,system:file:remove')")
    @DeleteMapping("/file-references/{ids}")
    public AjaxResult removeReferences(@PathVariable Long[] ids)
    {
        return toAjax(service.deleteFileReferences(ids));
    }
}
