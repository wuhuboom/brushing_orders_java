package com.order.system.service.impl;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.order.common.constant.CacheConstants;
import com.order.common.core.domain.model.LoginUser;
import com.order.common.core.redis.RedisCache;
import com.order.common.exception.ServiceException;
import com.order.common.utils.SecurityUtils;
import com.order.common.utils.StringUtils;
import com.order.common.utils.file.FileUtils;
import com.order.system.mapper.SystemAlignmentMapper;
import com.order.system.service.ISystemAlignmentService;

@Service
public class SystemAlignmentServiceImpl implements ISystemAlignmentService
{
    @Autowired
    private SystemAlignmentMapper mapper;

    @Autowired
    private RedisCache redisCache;

    @Override
    public List<Map<String, Object>> selectStrategyList(Map<String, Object> params)
    {
        return mapper.selectStrategyList(params);
    }

    @Override
    public Map<String, Object> selectStrategy(Long id)
    {
        Map<String, Object> result = mapper.selectStrategyById(id);
        if (result != null)
        {
            result.put("menuIds", mapper.selectStrategyMenuIds(id));
        }
        return result;
    }

    @Override
    @Transactional
    public int insertStrategy(Map<String, Object> data)
    {
        requireText(data, "strategyCode", "策略代码不能为空");
        requireText(data, "strategyName", "策略名称不能为空");
        data.put("createBy", currentUser());
        try
        {
            int rows = mapper.insertStrategy(data);
            replaceStrategyMenus(longValue(data.get("strategyId")), longList(data.get("menuIds")));
            return rows;
        }
        catch (DuplicateKeyException ex)
        {
            throw new ServiceException("策略代码已存在");
        }
    }

    @Override
    @Transactional
    public int updateStrategy(Map<String, Object> data)
    {
        Long strategyId = requiredId(data, "strategyId", "策略ID不能为空");
        requireText(data, "strategyCode", "策略代码不能为空");
        requireText(data, "strategyName", "策略名称不能为空");
        List<Long> affectedUsers = mapper.selectUsersAffectedByStrategy(strategyId);
        data.put("updateBy", currentUser());
        try
        {
            int rows = mapper.updateStrategy(data);
            replaceStrategyMenus(strategyId, longList(data.get("menuIds")));
            invalidateUsers(affectedUsers);
            return rows;
        }
        catch (DuplicateKeyException ex)
        {
            throw new ServiceException("策略代码已存在");
        }
    }

    @Override
    @Transactional
    public int deleteStrategies(Long[] ids)
    {
        requireIds(ids);
        if (mapper.countStrategyUsage(ids) > 0)
        {
            throw new ServiceException("策略已被角色或分组使用，不能删除");
        }
        for (Long id : ids)
        {
            mapper.deleteStrategyMenus(id);
        }
        return mapper.deleteStrategies(ids);
    }

    @Override
    public List<Map<String, Object>> selectGroupList(Map<String, Object> params)
    {
        return mapper.selectGroupList(params);
    }

    @Override
    public Map<String, Object> selectGroup(Long id)
    {
        Map<String, Object> result = mapper.selectGroupById(id);
        if (result != null)
        {
            result.put("menuIds", mapper.selectGroupMenuIds(id));
            result.put("strategyIds", mapper.selectGroupStrategyIds(id));
        }
        return result;
    }

    @Override
    @Transactional
    public int insertGroup(Map<String, Object> data)
    {
        requireText(data, "groupName", "分组名称不能为空");
        data.put("createBy", currentUser());
        try
        {
            int rows = mapper.insertGroup(data);
            replaceGroupRelations(longValue(data.get("groupId")), data);
            return rows;
        }
        catch (DuplicateKeyException ex)
        {
            throw new ServiceException("分组名称已存在");
        }
    }

    @Override
    @Transactional
    public int updateGroup(Map<String, Object> data)
    {
        Long groupId = requiredId(data, "groupId", "分组ID不能为空");
        requireText(data, "groupName", "分组名称不能为空");
        List<Long> affectedUsers = mapper.selectUsersAffectedByGroup(groupId);
        data.put("updateBy", currentUser());
        try
        {
            int rows = mapper.updateGroup(data);
            replaceGroupRelations(groupId, data);
            invalidateUsers(affectedUsers);
            return rows;
        }
        catch (DuplicateKeyException ex)
        {
            throw new ServiceException("分组名称已存在");
        }
    }

    @Override
    @Transactional
    public int deleteGroups(Long[] ids)
    {
        requireIds(ids);
        if (mapper.countGroupUsage(ids) > 0)
        {
            throw new ServiceException("分组已关联用户，不能删除");
        }
        for (Long id : ids)
        {
            mapper.deleteGroupMenus(id);
            mapper.deleteGroupStrategies(id);
        }
        return mapper.deleteGroups(ids);
    }

    @Override
    public List<Long> selectUserGroupIds(Long userId)
    {
        return mapper.selectUserGroupIds(userId);
    }

    @Override
    @Transactional
    public int replaceUserGroups(Long userId, List<Long> groupIds)
    {
        mapper.deleteUserGroups(userId);
        int rows = isEmpty(groupIds) ? 0 : mapper.insertUserGroups(userId, distinct(groupIds));
        invalidateUsers(Collections.singletonList(userId));
        return rows;
    }

    @Override
    public List<Long> selectPostRoleIds(Long postId)
    {
        return mapper.selectPostRoleIds(postId);
    }

    @Override
    @Transactional
    public int replacePostRoles(Long postId, List<Long> roleIds)
    {
        List<Long> affectedUsers = mapper.selectUsersAffectedByPost(postId);
        mapper.deletePostRoles(postId);
        int rows = isEmpty(roleIds) ? 0 : mapper.insertPostRoles(postId, distinct(roleIds));
        invalidateUsers(affectedUsers);
        return rows;
    }

    @Override
    public Map<String, Object> selectRoleAlignment(Long roleId)
    {
        Map<String, Object> result = new HashMap<>();
        result.put("roleId", roleId);
        result.put("strategyIds", mapper.selectRoleStrategyIds(roleId));
        result.put("dataRules", mapper.selectRoleDataRules(roleId));
        return result;
    }

    @Override
    @Transactional
    public int replaceRoleAlignment(Long roleId, Map<String, Object> data)
    {
        mapper.deleteRoleStrategies(roleId);
        List<Long> strategyIds = longList(data.get("strategyIds"));
        int rows = isEmpty(strategyIds) ? 0 : mapper.insertRoleStrategies(roleId, distinct(strategyIds));
        mapper.deleteRoleDataRules(roleId);
        List<Map<String, Object>> rules = ruleList(data.get("dataRules"));
        validateRules(rules);
        if (!rules.isEmpty())
        {
            rows += mapper.insertRoleDataRules(roleId, rules);
        }
        invalidateUsers(selectUsersAffectedByRole(roleId));
        return rows;
    }

    @Override
    public List<String> selectEffectivePermissions(Long userId)
    {
        return mapper.selectEffectivePermissions(userId);
    }

    @Override
    public List<String> selectRoleEffectivePermissions(Long roleId)
    {
        return mapper.selectRoleEffectivePermissions(roleId);
    }

    @Override
    public List<Long> selectEffectiveMenuIds(Long userId)
    {
        return mapper.selectEffectiveMenuIds(userId);
    }

    @Override
    public boolean shouldHidePhone(Long userId)
    {
        return userId != null && userId != 1L && mapper.countPhoneMaskingRoles(userId) > 0;
    }

    @Override
    public List<Map<String, Object>> selectFileList(Map<String, Object> params)
    {
        return mapper.selectFileList(params);
    }

    @Override
    public Map<String, Object> selectFile(Long id)
    {
        return mapper.selectFileById(id);
    }

    @Override
    @Transactional
    public Map<String, Object> registerFile(Map<String, Object> data)
    {
        String storagePath = String.valueOf(data.get("storagePath"));
        if (StringUtils.isEmpty(storagePath) || !Files.isRegularFile(Path.of(storagePath)))
        {
            throw new ServiceException("上传文件不存在，无法建立文件索引");
        }
        data.putIfAbsent("bucket", "local");
        data.put("fileHash", sha256(Path.of(storagePath)));
        data.put("fileSize", pathSize(Path.of(storagePath)));
        data.put("createBy", currentUser());
        Map<String, Object> file = mapper.selectFileByHash(String.valueOf(data.get("bucket")), String.valueOf(data.get("fileHash")));
        if (file == null)
        {
            int inserted = mapper.insertFile(data);
            file = inserted > 0 ? mapper.selectFileById(longValue(data.get("fileId")))
                    : mapper.selectFileByHash(String.valueOf(data.get("bucket")), String.valueOf(data.get("fileHash")));
            if (inserted == 0 && file != null && !storagePath.equals(String.valueOf(file.get("storagePath"))))
            {
                FileUtils.deleteFile(storagePath);
            }
        }
        else if (!storagePath.equals(String.valueOf(file.get("storagePath"))))
        {
            FileUtils.deleteFile(storagePath);
        }
        Map<String, Object> reference = new HashMap<>();
        reference.put("fileId", file.get("fileId"));
        reference.put("referenceName", data.get("referenceName") == null ? "未命名文件" : String.valueOf(data.get("referenceName")));
        reference.put("referenceType", data.getOrDefault("referenceType", "GENERAL"));
        reference.put("referenceTargetId", data.get("referenceTargetId"));
        reference.put("createBy", currentUser());
        reference.put("remark", data.get("remark"));
        mapper.insertFileReference(reference);
        Map<String, Object> result = new HashMap<>(file);
        result.put("referenceId", reference.get("referenceId"));
        return result;
    }

    @Override
    @Transactional
    public int deleteFiles(Long[] ids)
    {
        requireIds(ids);
        if (mapper.countFileReferences(ids) > 0)
        {
            throw new ServiceException("文件仍存在引用，请先删除文件引用");
        }
        List<String> paths = new ArrayList<>();
        for (Long id : ids)
        {
            Map<String, Object> file = mapper.selectFileById(id);
            if (file != null && file.get("storagePath") != null)
            {
                paths.add(String.valueOf(file.get("storagePath")));
            }
        }
        int rows = mapper.deleteFiles(ids);
        if (rows > 0)
        {
            paths.forEach(FileUtils::deleteFile);
        }
        return rows;
    }

    @Override
    public List<Map<String, Object>> selectFileReferenceList(Map<String, Object> params)
    {
        return mapper.selectFileReferenceList(params);
    }

    @Override
    public Map<String, Object> selectFileReference(Long id)
    {
        return mapper.selectFileReferenceById(id);
    }

    @Override
    @Transactional
    public int insertFileReference(Map<String, Object> data)
    {
        requiredId(data, "fileId", "文件ID不能为空");
        if (mapper.selectFileById(longValue(data.get("fileId"))) == null)
        {
            throw new ServiceException("文件不存在");
        }
        data.put("createBy", currentUser());
        return mapper.insertFileReference(data);
    }

    @Override
    @Transactional
    public int deleteFileReferences(Long[] ids)
    {
        requireIds(ids);
        return mapper.deleteFileReferences(ids);
    }

    private void replaceStrategyMenus(Long strategyId, List<Long> menuIds)
    {
        mapper.deleteStrategyMenus(strategyId);
        if (!isEmpty(menuIds))
        {
            mapper.insertStrategyMenus(strategyId, distinct(menuIds));
        }
    }

    private void replaceGroupRelations(Long groupId, Map<String, Object> data)
    {
        mapper.deleteGroupMenus(groupId);
        List<Long> menuIds = longList(data.get("menuIds"));
        if (!isEmpty(menuIds)) mapper.insertGroupMenus(groupId, distinct(menuIds));
        mapper.deleteGroupStrategies(groupId);
        List<Long> strategyIds = longList(data.get("strategyIds"));
        if (!isEmpty(strategyIds)) mapper.insertGroupStrategies(groupId, distinct(strategyIds));
    }

    private List<Long> selectUsersAffectedByRole(Long roleId)
    {
        List<Long> users = new ArrayList<>();
        // A role may be assigned directly or through a position. Reuse an effective-menu query helper
        // is not possible here, so the mapper exposes affected users through the same role relation SQL.
        users.addAll(mapper.selectUsersAffectedByStrategyForRole(roleId));
        return distinct(users);
    }

    private void invalidateUsers(Collection<Long> userIds)
    {
        if (isEmpty(userIds)) return;
        Set<Long> targets = new HashSet<>(userIds);
        Collection<String> keys = redisCache.keys(CacheConstants.LOGIN_TOKEN_KEY + "*");
        if (keys == null) return;
        for (String key : keys)
        {
            LoginUser loginUser = redisCache.getCacheObject(key);
            if (loginUser != null && targets.contains(loginUser.getUserId()))
            {
                redisCache.deleteObject(key);
            }
        }
    }

    private String sha256(Path path)
    {
        try (InputStream in = Files.newInputStream(path))
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            for (int n; (n = in.read(buffer)) > 0;) digest.update(buffer, 0, n);
            StringBuilder value = new StringBuilder(64);
            for (byte b : digest.digest()) value.append(String.format("%02x", b));
            return value.toString();
        }
        catch (Exception ex)
        {
            throw new ServiceException("计算文件哈希失败：" + ex.getMessage());
        }
    }

    private long pathSize(Path path)
    {
        try { return Files.size(path); }
        catch (Exception ex) { throw new ServiceException("读取文件大小失败：" + ex.getMessage()); }
    }

    private void validateRules(List<Map<String, Object>> rules)
    {
        Set<Long> menus = new HashSet<>();
        for (Map<String, Object> rule : rules)
        {
            Long menuId = longValue(rule.get("menuId"));
            String scope = String.valueOf(rule.get("scopeType"));
            if (menuId == null || !Set.of("ALL", "DEPT_AND_CHILD", "DEPT", "SELF").contains(scope))
            {
                throw new ServiceException("数据权限规则不合法");
            }
            if (!menus.add(menuId)) throw new ServiceException("同一资源只能配置一条数据权限");
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> ruleList(Object value)
    {
        return value instanceof List<?> ? (List<Map<String, Object>>) value : new ArrayList<>();
    }

    private List<Long> longList(Object value)
    {
        if (!(value instanceof Collection<?> collection)) return new ArrayList<>();
        List<Long> result = new ArrayList<>();
        for (Object item : collection)
        {
            Long id = longValue(item);
            if (id != null) result.add(id);
        }
        return result;
    }

    private Long longValue(Object value)
    {
        if (value == null || StringUtils.isEmpty(String.valueOf(value))) return null;
        return value instanceof Number number ? number.longValue() : Long.valueOf(String.valueOf(value));
    }

    private Long requiredId(Map<String, Object> data, String key, String message)
    {
        Long id = longValue(data.get(key));
        if (id == null) throw new ServiceException(message);
        return id;
    }

    private void requireText(Map<String, Object> data, String key, String message)
    {
        if (data.get(key) == null || StringUtils.isEmpty(String.valueOf(data.get(key)).trim()))
        {
            throw new ServiceException(message);
        }
    }

    private void requireIds(Long[] ids)
    {
        if (ids == null || ids.length == 0) throw new ServiceException("请选择要操作的数据");
    }

    private String currentUser()
    {
        try { return SecurityUtils.getUsername(); }
        catch (Exception ignored) { return "system"; }
    }

    private <T> List<T> distinct(Collection<T> values)
    {
        return new ArrayList<>(new java.util.LinkedHashSet<>(values));
    }

    private boolean isEmpty(Collection<?> values)
    {
        return values == null || values.isEmpty();
    }
}
