package com.brushing.framework.init;

import com.brushing.common.core.redis.RedisCache;
import com.brushing.set.domain.OrderTradeControlConfig;
import com.brushing.set.service.IOrderTradeControlConfigService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class StartupTask implements ApplicationRunner{

    @Autowired
    private IOrderTradeControlConfigService controlConfigService;

    @Autowired
    private RedisCache redisCache;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        OrderTradeControlConfig orderTradeControlConfig = controlConfigService.selectOrderTradeControlConfigById(1L);
        redisCache.setCacheObject("trade_config",orderTradeControlConfig);
    }
}
