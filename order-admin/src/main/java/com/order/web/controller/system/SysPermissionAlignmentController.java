package com.order.web.controller.system;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
@RequestMapping("/system/permission")
public class SysPermissionAlignmentController extends BaseController
{
    @Autowired
    private ISystemAlignmentService service;

    @PreAuthorize("@ss.hasAnyPermi('system:strategy:list,system:role:list')")
    @GetMapping("/strategies/list")
    public TableDataInfo strategyList(@RequestParam Map<String, Object> params)
    {
        startPage();
        return getDataTable(service.selectStrategyList(params));
    }

    @PreAuthorize("@ss.hasAnyPermi('system:strategy:query,system:role:query')")
    @GetMapping("/strategies/{id}")
    public AjaxResult strategy(@PathVariable Long id)
    {
        return success(service.selectStrategy(id));
    }

    @Log(title = "策略管理", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasAnyPermi('system:strategy:add,system:role:add')")
    @PostMapping("/strategies")
    public AjaxResult addStrategy(@RequestBody Map<String, Object> data)
    {
        return toAjax(service.insertStrategy(data));
    }

    @Log(title = "策略管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasAnyPermi('system:strategy:edit,system:role:edit')")
    @PutMapping("/strategies")
    public AjaxResult editStrategy(@RequestBody Map<String, Object> data)
    {
        return toAjax(service.updateStrategy(data));
    }

    @Log(title = "策略管理", businessType = BusinessType.DELETE)
    @PreAuthorize("@ss.hasAnyPermi('system:strategy:remove,system:role:remove')")
    @DeleteMapping("/strategies/{ids}")
    public AjaxResult removeStrategies(@PathVariable Long[] ids)
    {
        return toAjax(service.deleteStrategies(ids));
    }

    @PreAuthorize("@ss.hasAnyPermi('system:group:list,system:role:list')")
    @GetMapping("/groups/list")
    public TableDataInfo groupList(@RequestParam Map<String, Object> params)
    {
        startPage();
        return getDataTable(service.selectGroupList(params));
    }

    @PreAuthorize("@ss.hasAnyPermi('system:group:query,system:role:query')")
    @GetMapping("/groups/{id}")
    public AjaxResult group(@PathVariable Long id)
    {
        return success(service.selectGroup(id));
    }

    @Log(title = "分组管理", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasAnyPermi('system:group:add,system:role:add')")
    @PostMapping("/groups")
    public AjaxResult addGroup(@RequestBody Map<String, Object> data)
    {
        return toAjax(service.insertGroup(data));
    }

    @Log(title = "分组管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasAnyPermi('system:group:edit,system:role:edit')")
    @PutMapping("/groups")
    public AjaxResult editGroup(@RequestBody Map<String, Object> data)
    {
        return toAjax(service.updateGroup(data));
    }

    @Log(title = "分组管理", businessType = BusinessType.DELETE)
    @PreAuthorize("@ss.hasAnyPermi('system:group:remove,system:role:remove')")
    @DeleteMapping("/groups/{ids}")
    public AjaxResult removeGroups(@PathVariable Long[] ids)
    {
        return toAjax(service.deleteGroups(ids));
    }

    @PreAuthorize("@ss.hasAnyPermi('system:user:query,system:user:edit')")
    @GetMapping("/users/{userId}/groups")
    public AjaxResult userGroups(@PathVariable Long userId)
    {
        return success(service.selectUserGroupIds(userId));
    }

    @Log(title = "用户分组", businessType = BusinessType.GRANT)
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @PutMapping("/users/{userId}/groups")
    public AjaxResult saveUserGroups(@PathVariable Long userId, @RequestBody List<Long> groupIds)
    {
        service.replaceUserGroups(userId, groupIds);
        return success();
    }

    @PreAuthorize("@ss.hasAnyPermi('system:post:query,system:post:edit')")
    @GetMapping("/posts/{postId}/roles")
    public AjaxResult postRoles(@PathVariable Long postId)
    {
        return success(service.selectPostRoleIds(postId));
    }

    @Log(title = "职位角色", businessType = BusinessType.GRANT)
    @PreAuthorize("@ss.hasPermi('system:post:edit')")
    @PutMapping("/posts/{postId}/roles")
    public AjaxResult savePostRoles(@PathVariable Long postId, @RequestBody List<Long> roleIds)
    {
        service.replacePostRoles(postId, roleIds);
        return success();
    }

    @PreAuthorize("@ss.hasAnyPermi('system:role:query,system:role:edit')")
    @GetMapping("/roles/{roleId}/alignment")
    public AjaxResult roleAlignment(@PathVariable Long roleId)
    {
        return success(service.selectRoleAlignment(roleId));
    }

    @Log(title = "角色策略与数据权限", businessType = BusinessType.GRANT)
    @PreAuthorize("@ss.hasPermi('system:role:edit')")
    @PutMapping("/roles/{roleId}/alignment")
    public AjaxResult saveRoleAlignment(@PathVariable Long roleId, @RequestBody Map<String, Object> data)
    {
        service.replaceRoleAlignment(roleId, data);
        return success();
    }
}
