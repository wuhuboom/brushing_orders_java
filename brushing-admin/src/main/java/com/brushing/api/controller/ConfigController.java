package com.brushing.api.controller;

import com.brushing.api.controller.vo.PageDto;
import com.brushing.common.config.BrushingConfig;
import com.brushing.common.core.controller.BaseController;
import com.brushing.common.core.domain.AjaxResult;
import com.brushing.common.core.page.TableDataInfo;
import com.brushing.common.utils.StringUtils;
import com.brushing.common.utils.file.FileUploadUtils;
import com.brushing.framework.config.ServerConfig;
import com.brushing.member.domain.OrderMemberLevel;
import com.brushing.member.service.IOrderMemberLevelService;
import com.brushing.set.domain.*;
import com.brushing.set.service.*;
import com.brushing.system.domain.SysNotice;
import com.brushing.system.domain.SysTimeZone;
import com.brushing.system.service.ISysNoticeService;
import com.brushing.system.service.ISysTimeZoneService;
import com.github.pagehelper.PageHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(
        name = "配置管理",
        description =
                "错误码对照表：\n" +
                        "701: No data （暂无数据）\n" +
                        "703: Upload failed （上传失败）"
)
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

    @Autowired
    private IOrderMemberLevelService levelService;

    @Autowired
    private ISysNoticeService noticeService;

    @Autowired
    private ServerConfig serverConfig;

    @Autowired
    private ISysTimeZoneService sysTimeZoneService;

    /**
     * 获取客服地址
     */
    @GetMapping("/getCustomerService")
    @Operation(
            summary = "获取客服地址",
            description =
                    "'name': '客服名称',\n" +
                            "'linkUrl': '跳转地址',\n" +
                            "'iconUrl': '图标URL',\n" +
                            "'sort': '排序',\n" +
                            "'status': '状态 0正常 1停用'"
    )
    public AjaxResult getCustomerService(){
        List<OrderCustomerService> orderCustomerServices =
                orderCustomerServiceService.selectOrderCustomerServiceList(null);
        if (orderCustomerServices.size() == 0){
            return AjaxResult.error(701, "No data");
        }
        return success(orderCustomerServices);
    }

    @GetMapping("/getEmailConfig")
    @Operation(
            summary = "获取邮件配置",
            description =
                    "'host': '邮件服务器地址',\n" +
                            "'username': '邮箱用户名',\n" +
                            "'password': '邮箱密码或授权码',\n" +
                            "'fromAddress': '发件人邮箱地址',\n" +
                            "'fromName': '发件人名称'"
    )
    public AjaxResult getEmailConfig(){
        OrderEmailConfig orderEmailConfig = orderEmailConfigService.selectOrderEmailConfigById(1L);
        if (StringUtils.isNull(orderEmailConfig)){
            return AjaxResult.error(701, "No data");
        }
        return success(orderEmailConfig);
    }

    @GetMapping("/getGlobalConfig")
    @Operation(
            summary = "获取基本配置",
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
            return AjaxResult.error(701, "No data");
        }
        return success(orderGlobalConfig);
    }

    @GetMapping("/getTradeConfig")
    @Operation(
            summary = "获取交易配置",
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
        OrderTradeControlConfig orderTradeControlConfig =
                orderTradeControlConfigService.selectOrderTradeControlConfigById(1L);
        if (StringUtils.isNull(orderTradeControlConfig)){
            return AjaxResult.error(701, "No data");
        }
        return success(orderTradeControlConfig);
    }

    @GetMapping("/getLevel")
    @Operation(
            summary = "获取VIP等级列表",
            description =
                    "'icon': '会员图标',\n" +
                            "'nameZh': '中文名称',\n" +
                            "'nameEn': '英文名称',\n" +
                            "'price': '价格',\n" +
                            "'autoUpgradeInviteCount': '自动升级需邀请人数',\n" +
                            "'commissionRatio': '佣金比例',\n" +
                            "'streakCommissionRatio': '连单佣金比例',\n" +
                            "'minBalance': '最低余额',\n" +
                            "'orderCount': '接单次数',\n" +
                            "'withdrawCount': '提现次数',\n" +
                            "'withdrawLimit': '提现限额',\n" +
                            "'minWithdrawAmount': '最低提现金额',\n" +
                            "'maxWithdrawAmount': '最高提现金额',\n" +
                            "'withdrawFee': '提现手续费',\n" +
                            "'withdrawOrderPerDay': '每天多少单可以提现',\n" +
                            "'descriptionZh': '中文描述',\n" +
                            "'descriptionEn': '英文描述'"
    )
    public AjaxResult getLevel(){
        List<OrderMemberLevel> orderMemberLevels = levelService.selectOrderMemberLevelList(null);
        if (StringUtils.isNull(orderMemberLevels)){
            return AjaxResult.error(701, "No data");
        }
        return success(orderMemberLevels);
    }

    @GetMapping("/getNoticeList")
    @Operation(summary = "获取公告列表" , description = "noticeTitle:标题，noticeContent：类容")
    public TableDataInfo getNoticeList(PageDto dto) {
        PageHelper.startPage(dto.getPageNum(), dto.getPageSize());
        List<SysNotice> list = noticeService.selectNoticeList(new SysNotice());
        return getDataTable(list);
    }

    @GetMapping("/getNotice/{id}")
    @Operation(summary = "获取公告详情" , description = "noticeTitle:标题，noticeContent：类容")
    public AjaxResult getNotice(@PathVariable("id") Long id) {
        SysNotice sysNotice = noticeService.selectNoticeById(id);
        if (StringUtils.isNull(sysNotice)){
            return AjaxResult.error(701, "No data");
        }
        return success(sysNotice);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "文件上传接口",
            description = "code:200 表示上传成功，fileName: 路径地址"
    )
    public AjaxResult uploadFile(
            @RequestPart("file") @Parameter(description = "上传的文件") MultipartFile file
    ) throws Exception {
        try {
            // 上传文件路径
            String filePath = BrushingConfig.getUploadPath();
            // 上传并返回新文件名称
            String fileName = FileUploadUtils.upload(filePath, file);
            String url = serverConfig.getUrl() + fileName;
            AjaxResult ajax = AjaxResult.success();
            ajax.put("fileName", fileName);
            ajax.put("url", url);  // 同时返回可访问 URL
            return ajax;
        } catch (Exception e) {
            return AjaxResult.error(703, "Upload failed: " + e.getMessage());
        }
    }

    @GetMapping("/getZoneActive")
    @Operation(summary = "获取系统时区")
    public AjaxResult getZoneActive(){
        SysTimeZone active = sysTimeZoneService.getActive();
        AjaxResult ajaxResult = new AjaxResult();
        ajaxResult.put("data", active);
        ajaxResult.put("code", 200);
        ajaxResult.put("msg", "The operation was successful");
        return ajaxResult;
    }
}
