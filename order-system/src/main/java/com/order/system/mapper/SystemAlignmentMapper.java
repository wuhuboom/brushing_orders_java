package com.order.system.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/** System-management alignment persistence. */
public interface SystemAlignmentMapper
{
    List<Map<String, Object>> selectStrategyList(Map<String, Object> params);
    Map<String, Object> selectStrategyById(Long id);
    int insertStrategy(Map<String, Object> data);
    int updateStrategy(Map<String, Object> data);
    int deleteStrategies(Long[] ids);
    int countStrategyUsage(Long[] ids);
    int deleteStrategyMenus(Long strategyId);
    int insertStrategyMenus(@Param("strategyId") Long strategyId, @Param("menuIds") List<Long> menuIds);
    List<Long> selectStrategyMenuIds(Long strategyId);

    List<Map<String, Object>> selectGroupList(Map<String, Object> params);
    Map<String, Object> selectGroupById(Long id);
    int insertGroup(Map<String, Object> data);
    int updateGroup(Map<String, Object> data);
    int deleteGroups(Long[] ids);
    int countGroupUsage(Long[] ids);
    int deleteGroupMenus(Long groupId);
    int insertGroupMenus(@Param("groupId") Long groupId, @Param("menuIds") List<Long> menuIds);
    int deleteGroupStrategies(Long groupId);
    int insertGroupStrategies(@Param("groupId") Long groupId, @Param("strategyIds") List<Long> strategyIds);
    List<Long> selectGroupMenuIds(Long groupId);
    List<Long> selectGroupStrategyIds(Long groupId);

    List<Long> selectUserGroupIds(Long userId);
    int deleteUserGroups(Long userId);
    int insertUserGroups(@Param("userId") Long userId, @Param("groupIds") List<Long> groupIds);
    List<Long> selectPostRoleIds(Long postId);
    int deletePostRoles(Long postId);
    int insertPostRoles(@Param("postId") Long postId, @Param("roleIds") List<Long> roleIds);
    List<Long> selectRoleStrategyIds(Long roleId);
    int deleteRoleStrategies(Long roleId);
    int insertRoleStrategies(@Param("roleId") Long roleId, @Param("strategyIds") List<Long> strategyIds);
    List<Map<String, Object>> selectRoleDataRules(Long roleId);
    int deleteRoleDataRules(Long roleId);
    int insertRoleDataRules(@Param("roleId") Long roleId, @Param("rules") List<Map<String, Object>> rules);

    List<String> selectEffectivePermissions(Long userId);
    List<String> selectRoleEffectivePermissions(Long roleId);
    List<Long> selectEffectiveMenuIds(Long userId);
    List<String> selectEffectiveDataScopes(@Param("userId") Long userId, @Param("permission") String permission);
    List<Long> selectUsersAffectedByStrategy(Long strategyId);
    List<Long> selectUsersAffectedByGroup(Long groupId);
    List<Long> selectUsersAffectedByPost(Long postId);
    List<Long> selectUsersAffectedByStrategyForRole(Long roleId);
    int countPostRoleUsage(Long roleId);
    int countPhoneMaskingRoles(Long userId);

    List<Map<String, Object>> selectFileList(Map<String, Object> params);
    Map<String, Object> selectFileById(Long id);
    Map<String, Object> selectFileByHash(@Param("bucket") String bucket, @Param("hash") String hash);
    int insertFile(Map<String, Object> data);
    int updateFileStorageIfMatch(Map<String, Object> data);
    int deleteFiles(Long[] ids);
    int countFileReferences(Long[] ids);
    List<Map<String, Object>> selectFileReferenceList(Map<String, Object> params);
    Map<String, Object> selectFileReferenceById(Long id);
    int insertFileReference(Map<String, Object> data);
    int deleteFileReferences(Long[] ids);
}
