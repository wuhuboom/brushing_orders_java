package com.brushing.system.domain.vo;

import com.brushing.common.utils.StringUtils;

/**
 * 路由显示信息
 * 
 * @author brushing
 */
public class MetaVo
{
    /**
     * 设置该路由在侧边栏和面包屑中展示的名字
     */
    private String title;

    /**
     * 设置该路由的图标，对应路径src/assets/icons/svg
     */
    private String icon;

    /**
     * 设置为true，则不会被 <keep-alive>缓存
     */
    private boolean noCache;

    /**
     * 内链地址（http(s)://开头）
     */
    private String link;

    private String enName;

    public String getEnName() {
        return enName;
    }

    public void setEnName(String enName) {
        this.enName = enName;
    }

    public MetaVo()
    {
    }

    public MetaVo(String title,String enName, String icon)
    {
        this.title = title;
        this.enName = enName;
        this.icon = icon;
    }

    public MetaVo(String title, String enName,String icon, boolean noCache)
    {
        this.title = title;
        this.enName = enName;
        this.icon = icon;
        this.noCache = noCache;
    }

    public MetaVo(String title,String enName, String icon, String link)
    {
        this.title = title;
        this.enName = enName;
        this.icon = icon;
        this.link = link;
    }

    public MetaVo(String title, String enName, String icon, boolean noCache, String link)
    {
        this.title = title;
        this.enName = enName;
        this.icon = icon;
        this.noCache = noCache;
        if (StringUtils.ishttp(link))
        {
            this.link = link;
        }
    }

    public boolean isNoCache()
    {
        return noCache;
    }

    public void setNoCache(boolean noCache)
    {
        this.noCache = noCache;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getIcon()
    {
        return icon;
    }

    public void setIcon(String icon)
    {
        this.icon = icon;
    }

    public String getLink()
    {
        return link;
    }

    public void setLink(String link)
    {
        this.link = link;
    }
}
