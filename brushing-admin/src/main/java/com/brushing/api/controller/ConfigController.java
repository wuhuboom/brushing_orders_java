package com.brushing.api.controller;

import com.brushing.api.controller.vo.PageDto;
import com.brushing.common.annotation.Excel;
import com.brushing.common.config.BrushingConfig;
import com.brushing.common.core.controller.BaseController;
import com.brushing.common.core.domain.AjaxResult;
import com.brushing.common.core.page.TableDataInfo;
import com.brushing.common.core.redis.RedisCache;
import com.brushing.common.utils.StringUtils;
import com.brushing.common.utils.file.FileUploadUtils;
import com.brushing.framework.config.ServerConfig;
import com.brushing.framework.init.GeoIpQueryQueryService;
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

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(
        name = "配置管理",
        description =
                "错误码对照表：\n" +
                        "701: No data （暂无数据）\n" +
                        "703: Upload failed （上传失败）" +
                        "920:客户服务目前无法提供服务"
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
    private IOrderSiteConfigService siteConfigService;

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

    @Autowired
    private IOrderBannerService bannerService;

    @Autowired
    private RedisCache redisCache;



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
        OrderTradeControlConfig controlConfig = redisCache.getCacheObject("trade_config");
        LocalTime orderTimeStart = controlConfig.getWorkTimeStart();
        LocalTime orderTimeEnd = controlConfig.getWorkTimeEnd();
        SysTimeZone active = sysTimeZoneService.getActive();
        String tzName = active.getTzName();
        LocalTime now = LocalTime.now(ZoneId.of(tzName));
        if (!isWithinWithdrawTimeRange(now, orderTimeStart, orderTimeEnd)) {
            return AjaxResult.error(920, "Customer service is currently unavailable");
        }
        List<OrderCustomerService> orderCustomerServices =
                orderCustomerServiceService.selectOrderCustomerServiceList(null);
        if (orderCustomerServices.size() == 0){
            return AjaxResult.error(701, "No data");
        }
        return success(orderCustomerServices);
    }

    private boolean isWithinWithdrawTimeRange(LocalTime now, LocalTime start, LocalTime end) {
        return !now.isBefore(start) && !now.isAfter(end);
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

    @GetMapping("/getCustomerServiceByLang")
    @Operation(
            summary = "根据语言获取客服地址",
            description =
                    "**请求参数：**\n" +
                            "- `lang` (可选, string, 默认: en): 语言代码，支持 en（英文）、zh（简体中文）、zh_tw（繁体中文）、ja（日文）、th（泰文）、ko（韩文）。\n" +
                            "\n" +
                    "**返回字段：**\n" +
                    "'name': '客服名称',\n" +
                    "'linkUrl': '跳转地址',\n" +
                    "'iconUrl': '图标URL',\n" +
                    "'sort': '排序',\n" +
                    "'status': '状态 0正常 1停用'"
    )
    public AjaxResult getCustomerServiceByLang(@RequestParam(value = "lang", defaultValue = "en") @Parameter(description = "语言代码: en, zh, zh_tw, ja, th, ko ,pt") String lang) {
        if (!"en".equals(lang) && !"zh".equals(lang) && !"zh_tw".equals(lang) &&
                !"ja".equals(lang) && !"th".equals(lang) && !"ko".equals(lang)&& !"pt".equals(lang)) {
            return AjaxResult.error("不支持的语言参数，仅支持 en、zh、zh_tw、ja、th 或 ko ,pt");
        }

        OrderTradeControlConfig controlConfig = redisCache.getCacheObject("trade_config");
        LocalTime orderTimeStart = controlConfig.getWorkTimeStart();
        LocalTime orderTimeEnd = controlConfig.getWorkTimeEnd();
        SysTimeZone active = sysTimeZoneService.getActive();
        String tzName = active.getTzName();
        LocalTime now = LocalTime.now(ZoneId.of(tzName));
        if (!isWithinWithdrawTimeRange(now, orderTimeStart, orderTimeEnd)) {
            return AjaxResult.error(920, "Customer service is currently unavailable");
        }

        List<OrderCustomerService> orderCustomerServices = orderCustomerServiceService.selectOrderCustomerServiceList(null);
        if (orderCustomerServices.size() == 0) {
            return AjaxResult.error(701, "No data");
        }

        // 根据语言提取对应名称，构建响应列表
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (OrderCustomerService service : orderCustomerServices) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", getLocalizedName(service, lang));  // 提取语言特定名称
            item.put("linkUrl", service.getLinkUrl());
            item.put("iconUrl", service.getIconUrl());
            item.put("sort", service.getSort());
            item.put("status", service.getStatus());
            resultList.add(item);
        }
        return AjaxResult.success(resultList);
    }

    private String getLocalizedName(OrderCustomerService service, String lang) {
        switch (lang) {
            case "zh":
                return service.getNameZh() != null ? service.getNameZh() : service.getName();
            case "zh_tw":
                return service.getNameZhTw() != null ? service.getNameZhTw() : service.getName();
            case "ja":
                return service.getNameJp() != null ? service.getNameJp() : service.getName();
            case "th":
                return service.getNameTh() != null ? service.getNameTh() : service.getName();
            case "ko":
                return service.getNameKo() != null ? service.getNameKo() : service.getName();
            case "pt":
                return service.getNamePor() != null ? service.getNamePor() : service.getName();
            default:  // "en"
                return service.getName();
        }
    }

    @GetMapping("/getConfigByLang")
    @Operation(
            summary = "根据语言获取全局配置",
            description =
                    "接口描述：根据传入的语言参数返回对应的全局配置内容（仅返回7个核心配置项）。如果未提供 lang 参数，默认返回英文内容。\n" +
                            "\n" +
                            "**请求参数：**\n" +
                            "- `lang` (可选, string, 默认: en): 语言代码，支持 en（英文）、zh（简体中文）、zh_tw（繁体中文）、ja（日文）、th（泰文）、ko（韩文） pt（葡萄牙）,。\n" +
                            "\n" +
                            "**返回字段：**\n" +
                            "- `registerProtocol` (string): 注册协议内容\n" +
                            "- `aboutUs` (string): 关于我们内容\n" +
                            "- `certificate` (string): 证书内容\n" +
                            "- `faq` (string): 常见问题内容\n" +
                            "- `latestEvent` (string): 最新事件内容\n" +
                            "- `terms` (string): 条款条规内容\n" +
                            "- `incomeGuide` (string): 收入指南内容\n"
    )
    public AjaxResult getConfigByLang(@RequestParam(value = "lang", defaultValue = "en") String lang)
    {


        // 验证语言参数
        if (!"en".equals(lang) && !"zh".equals(lang) && !"zh_tw".equals(lang) &&
                !"ja".equals(lang) && !"th".equals(lang) && !"ko".equals(lang) && !"pt".equals(lang)) {
            return error("不支持的语言参数，仅支持 en、zh、zh_tw、ja、th 、 ko、pt");
        }

        // 查询所有全局配置（假设服务层有获取所有或单条的方法，根据实际调整）
        OrderGlobalConfig config = orderGlobalConfigService.selectOrderGlobalConfigById(1L);  // 示例：假设ID=1为默认配置，根据实际主键调整

        if (config == null) {
            return AjaxResult.error("未找到全局配置");
        }

        // 根据语言构建返回Map：key为配置类型，value为对应内容
        Map<String, String> result = new HashMap<>();

        if ("en".equals(lang)) {
            // 返回所有英文字段
            result.put("registerProtocol", config.getRegisterProtocolEn());
            result.put("aboutUs", config.getAboutUsEn());
            result.put("certificate", config.getCertificateEn());
            result.put("faq", config.getFaqEn());
            result.put("latestEvent", config.getLatestEventEn());
            result.put("terms", config.getTermsEn());
            result.put("incomeGuide", config.getIncomeGuideEn());
        } else if ("zh".equals(lang)) {
            // 返回简体中文（Local）字段
            result.put("registerProtocol", config.getRegisterProtocolLocal());
            result.put("aboutUs", config.getAboutUsLocal());
            result.put("certificate", config.getCertificateLocal());
            result.put("faq", config.getFaqLocal());
            result.put("latestEvent", config.getLatestEventLocal());
            result.put("terms", config.getTermsLocal());
            result.put("incomeGuide", config.getIncomeGuideLocal());
        } else if ("zh_tw".equals(lang)) {
            // 返回繁体中文（ZhTw）字段
            result.put("registerProtocol", config.getRegistrationAgreementZhTw());
            result.put("aboutUs", config.getAboutUsZhTw());
            result.put("certificate", config.getCertificateZhTw());
            result.put("faq", config.getFaqZhTw());
            result.put("latestEvent", config.getLatestEventsZhTw());
            result.put("terms", config.getTermsConditionsZhTw());
            result.put("incomeGuide", config.getIncomeGuideZhTw());
        } else if ("ja".equals(lang)) {
            // 返回日文（Ja）字段
            result.put("registerProtocol", config.getRegistrationAgreementJa());
            result.put("aboutUs", config.getAboutUsJa());
            result.put("certificate", config.getCertificateJa());
            result.put("faq", config.getFaqJa());
            result.put("latestEvent", config.getLatestEventsJa());
            result.put("terms", config.getTermsConditionsJa());
            result.put("incomeGuide", config.getIncomeGuideJa());
        } else if ("th".equals(lang)) {
            // 返回泰文（Th）字段
            result.put("registerProtocol", config.getRegistrationAgreementTh());
            result.put("aboutUs", config.getAboutUsTh());
            result.put("certificate", config.getCertificateTh());
            result.put("faq", config.getFaqTh());
            result.put("latestEvent", config.getLatestEventsTh());
            result.put("terms", config.getTermsConditionsTh());
            result.put("incomeGuide", config.getIncomeGuideTh());
        } else if ("ko".equals(lang)) {
            // 返回韩文（Ko）字段
            result.put("registerProtocol", config.getRegistrationAgreementKo());
            result.put("aboutUs", config.getAboutUsKo());
            result.put("certificate", config.getCertificateKo());
            result.put("faq", config.getFaqKo());
            result.put("latestEvent", config.getLatestEventsKo());
            result.put("terms", config.getTermsConditionsKo());
            result.put("incomeGuide", config.getIncomeGuideKo());
        }else if ("pt".equals(lang)) {
            // 返回韩文（Ko）字段
            result.put("registerProtocol", config.getRegistrationAgreementPor());
            result.put("aboutUs", config.getAboutUsPor());
            result.put("certificate", config.getCertificatePor());
            result.put("faq", config.getFaqPor());
            result.put("latestEvent", config.getLatestEventsPor());
            result.put("terms", config.getTermsConditionsPor());
            result.put("incomeGuide", config.getIncomeGuidePor());
        }

        return success(result);
    }

    @GetMapping("/getLevelByLang")
    @Operation(
            summary = "根据语言获取VIP等级列表",
            description =
                    "接口描述：根据传入的语言参数返回VIP等级列表（仅返回核心字段：会员图标、名称、价格及对应语言描述）。如果未提供 lang 参数，默认返回英文内容。\n" +
                            "\n" +
                            "**请求参数：**\n" +
                            "- `lang` (可选, string, 默认: en): 语言代码，支持 en（英文）、zh（简体中文）、zh_tw（繁体中文）、ja（日文）、th（泰文）、ko（韩文）、pt（葡萄牙）。\n" +
                            "\n" +
                            "**返回字段（每个等级对象）：**\n" +
                            "- `icon` (string): 会员图标\n" +
                            "- `name` (string): 等级名称（根据语言：zh返回中文名，其他返回英文名）\n" +
                            "- `price` (number): 价格\n" +
                            "- `description` (string): 描述内容（对应语言）\n"
    )
    public AjaxResult getLevelByLang(@RequestParam(value = "lang", defaultValue = "en") String lang) {
        // 验证语言参数
        if (!"en".equals(lang) && !"zh".equals(lang) && !"zh_tw".equals(lang) &&
                !"ja".equals(lang) && !"th".equals(lang) && !"ko".equals(lang) && !"pt".equals(lang)) {
            return error("不支持的语言参数，仅支持 en、zh、zh_tw、ja、th 、 ko、pt");
        }

        List<OrderMemberLevel> orderMemberLevels = levelService.selectOrderMemberLevelList(null);
        if (StringUtils.isNull(orderMemberLevels) || orderMemberLevels.isEmpty()) {
            return AjaxResult.error(701, "No data");
        }

        // 转换为简化Map列表
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (OrderMemberLevel level : orderMemberLevels) {
            Map<String, Object> levelMap = new HashMap<>();
            levelMap.put("icon", level.getIcon());

            // 名称：zh返回nameZh，其他返回nameEn
            String name = "zh".equals(lang) ? level.getNameZh() : level.getNameEn();
            levelMap.put("name", name);

            levelMap.put("price", level.getPrice());

            // 描述：根据语言选择对应字段
            String description = switch (lang) {
                case "zh" -> level.getDescriptionZh();
                case "zh_tw" -> level.getDescriptionZhTw();
                case "ja" -> level.getDescriptionJa();
                case "th" -> level.getDescriptionTh();
                case "ko" -> level.getDescriptionKo();
                case "pt" -> level.getDescriptionPor();
                default -> level.getDescriptionEn();
            };
            levelMap.put("description", description);

            resultList.add(levelMap);
        }

        return success(resultList);
    }



    @GetMapping("/getTradeConfig")
    @Operation(
            summary = "获取交易配置",
            description =
                    "'withdrawEnabled': '是否开启提现，0开启，1未开启',\n" +
                    "'minUserBalance': '用户交易最低余额',\n" +
                    "'minWithdrawCreditScore': '单笔提现最低信誉分',\n" +
                    "'minWithdrawAmount': '单笔提现最低金额',\n" +
                    "'maxWithdrawAmount': '单笔提现最高金额',\n" +
                    "'dailyWithdrawLimit': '单日总提现最大金额',\n" +
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

    @GetMapping("/bannerList")
    @Operation(summary = "获取轮播图", description = "name:轮播图名称，imageUrl: 图片地址，linkUrl：跳转地址，sort：排序")
    public AjaxResult getBannerList(){
        OrderBanner banner=new OrderBanner();
        banner.setStatus("0");
        List<OrderBanner> orderBanners = bannerService.selectOrderBannerList(banner);
        return success(orderBanners);
    }

    @GetMapping("/getEmailAddress")
    @Operation(summary = "获取邮箱地址")
    public AjaxResult getEmailAddress(){
        OrderSiteConfig orderSiteConfig = siteConfigService.selectOrderSiteConfigById(1L);
        String emailAddress = orderSiteConfig.getEmailAddress();
        AjaxResult ajaxResult=new AjaxResult();
        ajaxResult.put("code",200);
        ajaxResult.put("data",emailAddress);
        return ajaxResult;
    }

    @GetMapping("/getNoticeListByLang")
    @Operation(summary = "根据语言获取公告列表", description = "根据 lang 返回对应语言的标题与内容，若对应语言为空则回退到默认字段")
    public TableDataInfo getNoticeListByLang(PageDto dto, @RequestParam(value = "lang", defaultValue = "en") String lang) {
        PageHelper.startPage(dto.getPageNum(), dto.getPageSize());
        List<SysNotice> list = noticeService.selectNoticeList(new SysNotice());

        if (lang == null || (!"en".equals(lang) && !"zh".equals(lang) && !"zh_tw".equals(lang) && !"ja".equals(lang) && !"th".equals(lang) && !"ko".equals(lang) && !"pt".equals(lang))) {
            lang = "en";
        }

        for (SysNotice notice : list) {
            String localizedTitle;
            String localizedContent;
            switch (lang) {
                case "zh":
                    localizedTitle = notice.getTitleZh();
                    localizedContent = notice.getContentZh();
                    break;
                case "zh_tw":
                    localizedTitle = notice.getTitleZhTw();
                    localizedContent = notice.getContentZhTw();
                    break;
                case "ja":
                    localizedTitle = notice.getTitleJa();
                    localizedContent = notice.getContentJa();
                    break;
                case "th":
                    localizedTitle = notice.getTitleTh();
                    localizedContent = notice.getContentTh();
                    break;
                case "ko":
                    localizedTitle = notice.getTitleKo();
                    localizedContent = notice.getContentKo();
                    break;
                case "pt":
                    localizedTitle = notice.getTitlePor();
                    localizedContent = notice.getContentPor();
                    break;
                default:
                    localizedTitle = notice.getTitleEn();
                    localizedContent = notice.getContentEn();
            }

            if (localizedTitle == null || localizedTitle.isEmpty()) {
                localizedTitle = notice.getNoticeTitle();
            }
            if (localizedContent == null || localizedContent.isEmpty()) {
                localizedContent = notice.getNoticeContent();
            }

            notice.setNoticeTitle(localizedTitle);
            notice.setNoticeContent(localizedContent);
        }

        return getDataTable(list);
    }

    @GetMapping("/getNoticeByLang/{id}")
    @Operation(summary = "根据语言获取公告详情", description = "根据 lang 返回对应语言的标题与内容，若对应语言为空则回退到默认字段")
    public AjaxResult getNoticeByLang(
            @PathVariable("id") Long id,
            @RequestParam(value = "lang", defaultValue = "en") String lang) {
        SysNotice notice = noticeService.selectNoticeById(id);
        if (notice == null) {
            return AjaxResult.error(701, "No data");
        }

        String localizedTitle;
        String localizedContent;
        switch (lang) {
            case "zh":
                localizedTitle = notice.getTitleZh();
                localizedContent = notice.getContentZh();
                break;
            case "zh_tw":
                localizedTitle = notice.getTitleZhTw();
                localizedContent = notice.getContentZhTw();
                break;
            case "ja":
                localizedTitle = notice.getTitleJa();
                localizedContent = notice.getContentJa();
                break;
            case "th":
                localizedTitle = notice.getTitleTh();
                localizedContent = notice.getContentTh();
                break;
            case "ko":
                localizedTitle = notice.getTitleKo();
                localizedContent = notice.getContentKo();
                break;
            case "pt":
                localizedTitle = notice.getTitlePor();
                localizedContent = notice.getContentPor();
                break;
            default:
                localizedTitle = notice.getTitleEn();
                localizedContent = notice.getContentEn();
        }

        if (localizedTitle == null || localizedTitle.isEmpty()) {
            localizedTitle = notice.getNoticeTitle();
        }
        if (localizedContent == null || localizedContent.isEmpty()) {
            localizedContent = notice.getNoticeContent();
        }

        notice.setNoticeTitle(localizedTitle);
        notice.setNoticeContent(localizedContent);

        return AjaxResult.success(notice);
    }
}
