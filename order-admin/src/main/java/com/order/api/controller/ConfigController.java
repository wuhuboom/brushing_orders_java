package com.order.api.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageHelper;
import com.order.api.controller.dto.PageDto;
import com.order.common.config.OrderConfig;
import com.order.common.core.controller.BaseController;
import com.order.common.core.domain.AjaxResult;
import com.order.common.core.page.TableDataInfo;
import com.order.common.utils.StringUtils;
import com.order.common.utils.file.FileUploadUtils;
import com.order.framework.config.ServerConfig;
import com.order.member.domain.GoodsCustomerService;
import com.order.member.domain.GoodsMemberLevel;
import com.order.member.service.IGoodsCustomerServiceService;
import com.order.member.service.IGoodsMemberLevelService;
import com.order.member.service.IOrderConfigService;
import com.order.system.domain.SysNotice;
import com.order.system.domain.SysTimeZone;
import com.order.system.service.ISysNoticeService;
import com.order.system.service.ISysTimeZoneService;
import com.order.web.controller.tool.TimeRangeChecker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


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
    private ServerConfig serverConfig;

    @Autowired
    private IGoodsCustomerServiceService customerServiceService;

    @Autowired
    private ISysTimeZoneService sysTimeZoneService;

    @Autowired
    private IOrderConfigService orderConfigService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IGoodsMemberLevelService levelService;

    @Autowired
    private ISysNoticeService noticeService;





    @GetMapping("/getCustomerService")
    @Operation(
            summary = "获取客服地址",
            description = "### 返回字段说明\n\n" +
                    "- **name**: 名称\n" +
                    "- **sortOrder**: 序号\n" +
                    "- **image**: 图片（URL）\n" +
                    "- **isEnabled**: 是否启用\n" +
                    "- **link**: 链接（URL）\n"
    )
    public AjaxResult getCustomerService(){
        SysTimeZone active = sysTimeZoneService.getActive();
        Optional<Object> configValue = orderConfigService.getConfigValue("trade", "serviceTimeRange");
        boolean currentTimeInRange = TimeRangeChecker.isCurrentTimeInRange(configValue,active.getTzName());
        if (!currentTimeInRange) {
            return AjaxResult.error(920, "Customer service is currently unavailable");
        }
        List<GoodsCustomerService> goodsCustomerServices = customerServiceService.selectGoodsCustomerServiceList(null);
        if (goodsCustomerServices == null || goodsCustomerServices.isEmpty()) {
            return AjaxResult.error(701, "No data");
        }
        return AjaxResult.success(goodsCustomerServices);
    }


    @GetMapping("/getGlobalConfig")
    @Operation(
            summary = "获取基本配置",
            description = """
        ### 返回字段说明
        - **protocolContent**: 注册协议内容
        - **aboutContent**: 关于我们内容
        - **certificateContent**: 证书内容
        - **helpContent**: 帮助中心内容
        - **termsContent**: 条款内容
        - **eventContent**: 事件内容
        - **transactionDescription**: 交易描述
        - **orderDescription**: 订单描述
        - **usageDescription**: 使用说明
        **注**：如果配置不存在，返回该字段为 null。所有内容支持富文本渲染。
        """)
    public AjaxResult getGlobalConfig() {
        Map<String, Object> item = new HashMap<>();

        // 注册协议
        Optional<Object> protocolOpt = orderConfigService.getConfigValue("register", "protocolContent");
        item.put("protocolContent", protocolOpt.orElse(null));

        // 关于我们
        Optional<Object> aboutOpt = orderConfigService.getConfigValue("about", "aboutContent");
        item.put("aboutContent", aboutOpt.orElse(null));

        // 证书
        Optional<Object> certificateOpt = orderConfigService.getConfigValue("certificate", "certificateContent");
        item.put("certificateContent", certificateOpt.orElse(null));

        // 帮助中心
        Optional<Object> helpOpt = orderConfigService.getConfigValue("help", "helpContent");
        item.put("helpContent", helpOpt.orElse(null));

        // 条款
        Optional<Object> termsOpt = orderConfigService.getConfigValue("terms", "termsContent");
        item.put("termsContent", termsOpt.orElse(null));

        // 事件
        Optional<Object> eventOpt = orderConfigService.getConfigValue("event", "eventContent");
        item.put("eventContent", eventOpt.orElse(null));

        // 交易描述
        Optional<Object> transactionOpt = orderConfigService.getConfigValue("transaction", "transactionDescription");
        item.put("transactionDescription", transactionOpt.orElse(null));

        // 订单描述
        Optional<Object> orderOpt = orderConfigService.getConfigValue("order", "orderDescription");
        item.put("orderDescription", orderOpt.orElse(null));

        // 使用说明
        Optional<Object> usageOpt = orderConfigService.getConfigValue("usage", "usageDescription");
        item.put("usageDescription", usageOpt.orElse(null));

        return AjaxResult.success(item);
    }


    @GetMapping("/getTradeConfig")
    @Operation(
            summary = "获取交易配置",
            description = """
            ### 返回字段说明
            - **registerBonusAmount**: 注册奖金金额
            - **minTradeBalance**: 最低交易余额
            - **memberWithdrawalStatus**: 会员提现状态
            - **minCreditScoreForWithdrawal**: 提现最低信用分
            - **minWithdrawalAmount**: 最低提现金额
            - **maxWithdrawalAmount**: 最高提现金额
            - **platformDailyMaxWithdrawal**: 平台每日最高提现总额
            - **withdrawalFeeRate**: 提现手续费率
            - **parentRebatePercentage**: 上级返佣百分比
            - **matchRangePercentage**: 匹配范围百分比
            - **serviceTimeRange**: 服务时间范围
            - **tradeTimeRange**: 交易时间范围
            - **prohibitWithdrawalAfterRecharge**: 充值后禁止提现
            - **withdrawalRestrictLevelMinBalance**: 提现限制等级最低余额
            - **withdrawalTimeRange**: 提现时间范围
            - **autoSubmitTask**: 自动提交任务
            - **startTaskDelayMs**: 开始任务延迟（毫秒）
            - **submitTaskDelayMs**: 提交任务延迟（毫秒）
            - **orderExpireSeconds**: 订单过期秒数
            - **lockExtraCommissionOnSubmit**: 提交时锁定额外佣金
            - **allowModifyWithdrawalAddress**: 允许修改提现地址
            - **requiredTaskGroupsForWithdrawal**: 提现所需任务组数
            - **rechargeBonusTradeType**: 充值赠送交易类型
            - **includeContinuousOrderInTaskProgress**: 任务进度是否计算连单明细
            - **includePendingTasksInProgress**: 任务进度是否包含待提交任务
            - **maxPasswordFailuresForWithdrawal**: 禁止客户提现所需交易密码失败次数 (0-不限制)

            **注**：如果配置不存在，返回空对象 `{}`。
            """)
    public AjaxResult getTradeConfig() {
        com.order.member.domain.OrderConfig tradeConfig = orderConfigService.selectOrderConfigByType("trade");
        Map<String, Object> configMap = new HashMap<>();

        if (tradeConfig != null && tradeConfig.getContent() != null && !tradeConfig.getContent().isEmpty()) {
            try {
                // 解析 JSON content 为 Map
                configMap = objectMapper.readValue(tradeConfig.getContent(), Map.class);
            } catch (JsonProcessingException e) {
                // 可选：日志记录 "JSON 解析失败: " + e.getMessage();
                // 如果解析失败，返回空 Map
            }
        }
        // 如果 null 或空，返回空 Map

        return AjaxResult.success(configMap);
    }

    @GetMapping("/getLevel")
    @Operation(
            summary = "获取VIP等级列表",
           description = """
        ### 返回字段说明
        - **id**: 等级ID
        - **name**: 名称
        - **level**: 等级数值
        - **icon**: 图标URL
        - **price**: 等级价格
        - **minBalance**: 最低余额
        - **inviteCount**: 自动升级所需邀请人数
        - **orderCountPerDay**: 接单次数/天
        - **minCommissionRate**: 最低返佣百分比
        - **maxCommissionRate**: 最高返佣百分比
        - **taskCountPerDay**: 任务完成组数/天
        - **withdrawCountPerDay**: 提现次数/天
        - **withdrawFeeRate**: 提现手续费率
        - **minWithdraw**: 最低提现金额
        - **maxWithdraw**: 最高提现金额
        - **description**: 描述
        """)
    public AjaxResult getLevel(){
        List<GoodsMemberLevel> goodsMemberLevels = levelService.selectGoodsMemberLevelList(null);
        if (StringUtils.isNull(goodsMemberLevels)){
            return AjaxResult.error(701, "No data");
        }
        return success(goodsMemberLevels);
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
            String filePath = OrderConfig.getUploadPath();
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
}
