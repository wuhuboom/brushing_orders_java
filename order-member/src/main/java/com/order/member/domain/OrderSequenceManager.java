package com.order.member.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.order.common.annotation.Excel;
import com.order.common.core.domain.BaseEntity;

/**
 * 序列管理对象 order_sequence_manager
 * 
 * @author order
 * @date 2025-10-25
 */
public class OrderSequenceManager extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 自增ID */
    private Long id;

    /** 编码类型: e.g., FLOW_NO, TRADE_NO */
    @Excel(name = "编码类型: e.g., FLOW_NO, TRADE_NO")
    private String seqType;

    /** 日期: YYYYMMDD */
    @Excel(name = "日期: YYYYMMDD")
    private String seqDate;

    /** 时间: HHMM */
    @Excel(name = "时间: HHMM")
    private String seqTime;

    /** 当前序列值 */
    @Excel(name = "当前序列值")
    private Long currentSeq;

    /** 乐观锁版本 */
    @Excel(name = "乐观锁版本")
    private Long version;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setSeqType(String seqType) 
    {
        this.seqType = seqType;
    }

    public String getSeqType() 
    {
        return seqType;
    }

    public void setSeqDate(String seqDate) 
    {
        this.seqDate = seqDate;
    }

    public String getSeqDate() 
    {
        return seqDate;
    }

    public void setSeqTime(String seqTime) 
    {
        this.seqTime = seqTime;
    }

    public String getSeqTime() 
    {
        return seqTime;
    }

    public void setCurrentSeq(Long currentSeq) 
    {
        this.currentSeq = currentSeq;
    }

    public Long getCurrentSeq() 
    {
        return currentSeq;
    }

    public void setVersion(Long version) 
    {
        this.version = version;
    }

    public Long getVersion() 
    {
        return version;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("seqType", getSeqType())
            .append("seqDate", getSeqDate())
            .append("seqTime", getSeqTime())
            .append("currentSeq", getCurrentSeq())
            .append("version", getVersion())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
