package com.order.member.mapper;

import java.util.List;
import com.order.member.domain.OrderSequenceManager;
import org.apache.ibatis.annotations.Param;

/**
 * 序列管理Mapper接口
 * 
 * @author order
 * @date 2025-10-25
 */
public interface OrderSequenceManagerMapper 
{
    /**
     * 查询序列管理
     * 
     * @param id 序列管理主键
     * @return 序列管理
     */
    public OrderSequenceManager selectOrderSequenceManagerById(Long id);

    /**
     * 查询序列管理列表
     * 
     * @param orderSequenceManager 序列管理
     * @return 序列管理集合
     */
    public List<OrderSequenceManager> selectOrderSequenceManagerList(OrderSequenceManager orderSequenceManager);

    /**
     * 新增序列管理
     * 
     * @param orderSequenceManager 序列管理
     * @return 结果
     */
    public int insertOrderSequenceManager(OrderSequenceManager orderSequenceManager);

    /**
     * 修改序列管理
     * 
     * @param orderSequenceManager 序列管理
     * @return 结果
     */
    public int incrementWithVersion(OrderSequenceManager orderSequenceManager);

    /**
     * 删除序列管理
     * 
     * @param id 序列管理主键
     * @return 结果
     */
    public int deleteOrderSequenceManagerById(Long id);

    /**
     * 批量删除序列管理
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteOrderSequenceManagerByIds(Long[] ids);

    // 新增：获取当前序列详情（用于乐观锁）
    public OrderSequenceManager getCurrentSeq(@Param("seqType") String seqType, @Param("seqDate") String seqDate, @Param("seqTime") String seqTime);

    /**
     * 按 key 直接自增 current_seq（用于 JVM 端加锁的场景），返回受影响行数
     */
    public int incrementByKey(@Param("seqType") String seqType, @Param("seqDate") String seqDate, @Param("seqTime") String seqTime, @Param("updateTime") java.util.Date updateTime);

    /**
     * 原子自增并返回新值（MySQL: INSERT ... ON DUPLICATE KEY UPDATE + LAST_INSERT_ID）。
     * 需要 DB 表上对 (seq_type, seq_date, seq_time) 加 UNIQUE 索引。
     */
    public Long getAndIncrementAtomic(@Param("seqType") String seqType, @Param("seqDate") String seqDate, @Param("seqTime") String seqTime);

    // 新增：获取当前序列详情（用于事务性锁定）
    public OrderSequenceManager getCurrentSeqForUpdate(@Param("seqType") String seqType, @Param("seqDate") String seqDate, @Param("seqTime") String seqTime);

    // 新增：通过ID更新current_seq
    public int updateCurrentSeqById(OrderSequenceManager orderSequenceManager);

}
