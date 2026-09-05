package com.order.system.domain.dto;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 用户表格列配置请求与响应。
 */
public class TableColumnConfigDto
{
    /** 配置结构版本 */
    private Integer version;

    /** 按当前显示顺序保存的列 */
    private List<Item> items;

    @NotNull(message = "表格列配置版本不能为空")
    public Integer getVersion()
    {
        return version;
    }

    public void setVersion(Integer version)
    {
        this.version = version;
    }

    @Valid
    @NotNull(message = "表格列配置项不能为空")
    @Size(max = 200, message = "表格列配置项不能超过200个")
    public List<Item> getItems()
    {
        return items;
    }

    public void setItems(List<Item> items)
    {
        this.items = items;
    }

    /** 单列配置 */
    public static class Item
    {
        private String id;
        private Boolean visible;
        private String fixed;

        @NotBlank(message = "列标识不能为空")
        @Size(max = 128, message = "列标识不能超过128个字符")
        public String getId()
        {
            return id;
        }

        public void setId(String id)
        {
            this.id = id;
        }

        @NotNull(message = "列显示状态不能为空")
        public Boolean getVisible()
        {
            return visible;
        }

        public void setVisible(Boolean visible)
        {
            this.visible = visible;
        }

        @Pattern(regexp = "left|right", message = "列固定位置只能是left、right或null")
        public String getFixed()
        {
            return fixed;
        }

        public void setFixed(String fixed)
        {
            this.fixed = fixed;
        }
    }
}
