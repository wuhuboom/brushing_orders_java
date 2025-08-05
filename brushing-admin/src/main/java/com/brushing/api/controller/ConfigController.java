package com.brushing.api.controller;

import com.brushing.common.core.controller.BaseController;
import com.brushing.common.core.domain.AjaxResult;
import com.brushing.common.utils.StringUtils;
import com.brushing.set.domain.*;
import com.brushing.set.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@Tag(name = "配置管理")
@RestController
@RequestMapping("/api/config")
public class ConfigController extends BaseController {

    @Autowired
    private IOrderCustomerServiceService orderCustomerServiceService;

    @Autowired
    private IOrderEmailConfigService orderEmailConfigService;

    @Autowired
    private IOrderGlobalConfigService orderGlobalConfigService;

    @Autowired
    private IOrderTradeControlConfigService orderTradeControlConfigService;

    /**
     * 获取客服地址
     * @return
     */
    @GetMapping("/getCustomerService")
    @Operation(summary = "获取客服地址",
            description =
                    "'name': '客服名称',\n" +
                            "'linkUrl': '跳转地址',\n" +
                            "'iconUrl': '图标URL',\n" +
                            "'sort': '排序',\n" +
                            "'status': '状态 0正常 1停用'"
    )
    public AjaxResult getCustomerService(){
        List<OrderCustomerService> orderCustomerServices = orderCustomerServiceService.selectOrderCustomerServiceList(null);
        if (orderCustomerServices.size()==0){
            return error("No data yet");
        }
        return success(orderCustomerServices);
    }

    @GetMapping("/getEmailConfig")
    @Operation(summary = "获取邮件配置",
            description =
                    "'host': '邮件服务器地址,\n" +
                    "'username': '邮箱用户名',\n" +
                    "'password': '邮箱密码或授权码',\n" +
                    "'fromAddress': '发件人邮箱地址',\n" +
                    "'fromName': '发件人名称'"
    )
    public AjaxResult getEmailConfig(){
        OrderEmailConfig orderEmailConfig = orderEmailConfigService.selectOrderEmailConfigById(1L);
        if (StringUtils.isNull(orderEmailConfig)){
            return error("No data yet");
        }
        return success(orderEmailConfig);
    }

    @GetMapping("/getGlobalConfig")
    @Operation(summary = "获取基本配置",
            description =
                    "'registerProtocolEn': '注册协议（英文）',\n" +
                    "'registerProtocolLocal': '注册协议（非英文）',\n" +
                    "'aboutUsEn': '关于我们（英文）',\n" +
                    "'aboutUsLocal': '关于我们（非英文）',\n" +
                    "'certificateEn': '证书（英文）',\n" +
                    "'certificateLocal': '证书（非英文）',\n" +
                    "'faqEn': '常见问题（英文）',\n" +
                    "'faqLocal': '常见问题（非英文）',\n" +
                    "'latestEventEn': '最新事件（英文）',\n" +
                    "'latestEventLocal': '最新事件（非英文）',\n" +
                    "'termsEn': '条款条规（英文）',\n" +
                    "'termsLocal': '条款条规（非英文）',\n" +
                    "'incomeGuideEn': '收入指南（英文）',\n" +
                    "'incomeGuideLocal': '收入指南（非英文）'"
    )
    public AjaxResult getGlobalConfig(){
        OrderGlobalConfig orderGlobalConfig = orderGlobalConfigService.selectOrderGlobalConfigById(1L);
        if (StringUtils.isNull(orderGlobalConfig)){
            return error("No data yet");
        }
        return success(orderGlobalConfig);
    }

    @GetMapping("/getTradeConfig")
    @Operation(summary = "获取交易配置",
            description =
                    "'rechargeTimeStart': '充值时间开始',\n" +
                    "'rechargeTimeEnd': '充值时间结束',\n" +
                    "'withdrawTimeStart': '提现时间开始',\n" +
                    "'withdrawTimeEnd': '提现时间结束',\n" +
                    "'orderTimeStart': '抢单时间开始',\n" +
                    "'orderTimeEnd': '抢单时间结束',\n" +
                    "'workTimeStart': '工作时间开始',\n" +
                    "'workTimeEnd': '工作时间结束'"
)
    public AjaxResult getTradeConfig(){
        OrderTradeControlConfig orderTradeControlConfig = orderTradeControlConfigService.selectOrderTradeControlConfigById(1L);
        if (StringUtils.isNull(orderTradeControlConfig)){
            return error("No data yet");
        }
        return success(orderTradeControlConfig);

    }
}
