package com.order.system.service;

import java.util.List;
import java.util.Map;

public interface ISystemAlignmentService
{
    List<Map<String, Object>> selectStrategyList(Map<String, Object> params);
    Map<String, Object> selectStrategy(Long id);
    int insertStrategy(Map<String, Object> data);
    int updateStrategy(Map<String, Object> data);
    int deleteStrategies(Long[] ids);

    List<Map<String, Object>> selectGroupList(Map<String, Object> params);
    Map<String, Object> selectGroup(Long id);
    int insertGroup(Map<String, Object> data);
    int updateGroup(Map<String, Object> data);
    int deleteGroups(Long[] ids);

    List<Long> selectUserGroupIds(Long userId);
    int replaceUserGroups(Long userId, List<Long> groupIds);
    List<Long> selectPostRoleIds(Long postId);
    int replacePostRoles(Long postId, List<Long> roleIds);
    Map<String, Object> selectRoleAlignment(Long roleId);
    int replaceRoleAlignment(Long roleId, Map<String, Object> data);

    List<String> selectEffectivePermissions(Long userId);
    List<String> selectRoleEffectivePermissions(Long roleId);
    List<Long> selectEffectiveMenuIds(Long userId);
    boolean shouldHidePhone(Long userId);

    List<Map<String, Object>> selectFileList(Map<String, Object> params);
    Map<String, Object> selectFile(Long id);
    Map<String, Object> registerFile(Map<String, Object> data);
    int deleteFiles(Long[] ids);
    List<Map<String, Object>> selectFileReferenceList(Map<String, Object> params);
    Map<String, Object> selectFileReference(Long id);
    int insertFileReference(Map<String, Object> data);
    int deleteFileReferences(Long[] ids);
}
