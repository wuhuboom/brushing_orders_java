package com.order.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.order.common.core.domain.entity.SysUser;

/**
 * 用户表 数据层
 * 
 * @author order
 */
public interface SysUserMapper
{
    /**
     * 根据条件分页查询用户列表
     * 
     * @param sysUser 用户信息
     * @return 用户信息集合信息
     */
    public List<SysUser> selectUserList(SysUser sysUser);

    /**
     * 根据条件分页查询已配用户角色列表
     * 
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    public List<SysUser> selectAllocatedList(SysUser user);

    /**
     * 根据条件分页查询未分配用户角色列表
     * 
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    public List<SysUser> selectUnallocatedList(SysUser user);

    /**
     * 通过用户名查询用户
     * 
     * @param userName 用户名
     * @return 用户对象信息
     */
    public SysUser selectUserByUserName(String userName);

    /**
     * 通过用户ID查询用户
     * 
     * @param userId 用户ID
     * @return 用户对象信息
     */
    public SysUser selectUserById(Long userId);

    /**
     * 新增用户信息
     * 
     * @param user 用户信息
     * @return 结果
     */
    public int insertUser(SysUser user);

    /**
     * 修改用户信息
     * 
     * @param user 用户信息
     * @return 结果
     */
    public int updateUser(SysUser user);

    /**
     * 修改用户头像
     * 
     * @param userId 用户ID
     * @param avatar 头像地址
     * @return 结果
     */
    public int updateUserAvatar(@Param("userId") Long userId, @Param("avatar") String avatar);

    /**
     * 重置用户密码
     * 
     * @param userId 用户ID
     * @param password 密码
     * @return 结果
     */
    public int resetUserPwd(@Param("userId") Long userId, @Param("password") String password);

    /**
     * 专用：重置用户的 Google 验证字段（清空 secret 并将 google_enabled/google_auth_enabled 设为未启用）
     *
     * @param userId 用户ID
     * @param updateBy 操作人
     * @return 结果行数
     */
    public int resetUserGoogleAuth(@Param("userId") Long userId, @Param("updateBy") String updateBy);

    int configureUserGoogleAuth(@Param("userId") Long userId, @Param("enabled") String enabled, @Param("updateBy") String updateBy);

    int unlockUsers(Long[] userIds);

    /**
     * 专用：为用户持久化 Google 验证密钥（加密），并标记为已启用/已绑定
     *
     * @param userId 用户ID
     * @param encryptedSecret 加密后的 secret
     * @param updateBy 操作人
     * @return 受影响行数
     */
    int updateUserGoogleAuthSecret(@Param("userId") Long userId, @Param("encryptedSecret") String encryptedSecret, @Param("updateBy") String updateBy);

    /**
     * 通过用户ID删除用户
     * 
     * @param userId 用户ID
     * @return 结果
     */
    public int deleteUserById(Long userId);

    /**
     * 批量删除用户信息
     * 
     * @param userIds 需要删除的用户ID
     * @return 结果
     */
    public int deleteUserByIds(Long[] userIds);

    /**
     * 校验用户名称是否唯一
     * 
     * @param userName 用户名称
     * @return 结果
     */
    public SysUser checkUserNameUnique(String userName);

    /**
     * 校验手机号码是否唯一
     *
     * @param phonenumber 手机号码
     * @return 结果
     */
    public SysUser checkPhoneUnique(String phonenumber);

    /**
     * 校验email是否唯一
     *
     * @param email 用户邮箱
     * @return 结果
     */
    public SysUser checkEmailUnique(String email);
}
