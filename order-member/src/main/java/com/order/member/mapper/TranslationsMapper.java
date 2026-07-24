package com.order.member.mapper;

import java.util.List;
import com.order.common.i18n.Translations;

/**
 * 多语言翻译Mapper接口
 * 
 * @author order
 * @date 2025-12-16
 */
public interface TranslationsMapper 
{
    /**
     * 查询多语言翻译
     * 
     * @param id 多语言翻译主键
     * @return 多语言翻译
     */
    public Translations selectTranslationsById(Long id);

    public List<Translations> selectTranslationsByIds(List<Long> ids);

    /**
     * 查询多语言翻译列表
     * 
     * @param translations 多语言翻译
     * @return 多语言翻译集合
     */
    public List<Translations> selectTranslationsList(Translations translations);

    /**
     * 新增多语言翻译
     * 
     * @param translations 多语言翻译
     * @return 结果
     */
    public int insertTranslations(Translations translations);

    /**
     * 修改多语言翻译
     * 
     * @param translations 多语言翻译
     * @return 结果
     */
    public int updateTranslations(Translations translations);

    /**
     * 删除多语言翻译
     * 
     * @param id 多语言翻译主键
     * @return 结果
     */
    public int deleteTranslationsById(Long id);

    /**
     * 批量删除多语言翻译
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteTranslationsByIds(Long[] ids);
}
