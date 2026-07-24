package com.order.member.service.impl;

import java.util.List;
import com.order.common.i18n.ITranslationsService;
import com.order.common.i18n.Translations;
import com.order.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.order.member.mapper.GoodsCustomerServiceMapper;
import com.order.member.domain.GoodsCustomerService;
import com.order.member.service.IGoodsCustomerServiceService;

/**
 * 客服Service业务层处理
 * 
 * @author order
 * @date 2025-11-11
 */
@Service
@Transactional
public class GoodsCustomerServiceServiceImpl implements IGoodsCustomerServiceService 
{
    @Autowired
    private GoodsCustomerServiceMapper goodsCustomerServiceMapper;

    @Autowired
    private ITranslationsService translationsService;

    /**
     * 查询客服
     * 
     * @param id 客服主键
     * @return 客服
     */
    @Override
    public GoodsCustomerService selectGoodsCustomerServiceById(String id)
    {
        GoodsCustomerService service = goodsCustomerServiceMapper.selectGoodsCustomerServiceById(id);
        if (service != null && service.getTranslationsId() != null) {
            service.setTranslations(translationsService.selectTranslationsById(service.getTranslationsId()));
        }
        return service;
    }

    /**
     * 查询客服列表
     * 
     * @param goodsCustomerService 客服
     * @return 客服
     */
    @Override
    public List<GoodsCustomerService> selectGoodsCustomerServiceList(GoodsCustomerService goodsCustomerService)
    {
        return goodsCustomerServiceMapper.selectGoodsCustomerServiceList(goodsCustomerService);
    }

    /**
     * 新增客服
     * 
     * @param goodsCustomerService 客服
     * @return 结果
     */
    @Override
    public int insertGoodsCustomerService(GoodsCustomerService goodsCustomerService)
    {
        saveTranslations(goodsCustomerService);
        goodsCustomerService.setCreateTime(DateUtils.getNowDate());
        return goodsCustomerServiceMapper.insertGoodsCustomerService(goodsCustomerService);
    }

    /**
     * 修改客服
     * 
     * @param goodsCustomerService 客服
     * @return 结果
     */
    @Override
    public int updateGoodsCustomerService(GoodsCustomerService goodsCustomerService)
    {
        saveTranslations(goodsCustomerService);
        return goodsCustomerServiceMapper.updateGoodsCustomerService(goodsCustomerService);
    }

    /**
     * 批量删除客服
     * 
     * @param ids 需要删除的客服主键
     * @return 结果
     */
    @Override
    public int deleteGoodsCustomerServiceByIds(String[] ids)
    {
        return goodsCustomerServiceMapper.deleteGoodsCustomerServiceByIds(ids);
    }

    /**
     * 删除客服信息
     * 
     * @param id 客服主键
     * @return 结果
     */
    @Override
    public int deleteGoodsCustomerServiceById(String id)
    {
        return goodsCustomerServiceMapper.deleteGoodsCustomerServiceById(id);
    }

    private void saveTranslations(GoodsCustomerService service) {
        Translations translations = service.getTranslations();
        if (translations == null || (translations.getId() == null && !translations.hasAnyValue())) {
            return;
        }
        if (translations.getId() == null) {
            translationsService.insertTranslations(translations);
        } else {
            translationsService.updateTranslations(translations);
        }
        service.setTranslationsId(translations.getId());
    }
}
