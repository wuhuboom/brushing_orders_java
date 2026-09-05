package com.order.web.core.config;

import com.order.api.controller.dto.AccountApiDtos;
import com.order.api.controller.dto.ConfigApiDtos;
import com.order.api.controller.dto.OrderApiDtos;
import com.order.api.controller.dto.UserProfileResponse;
import com.order.api.service.UserLoginResult;
import com.order.member.domain.Goods;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 为 H5 接口补充实际的响应结构、中文字段说明和业务状态码说明。
 *
 * <p>控制器为了兼容现有 H5 客户端仍返回 {@code AjaxResult}/{@code TableDataInfo}，
 * Springdoc 无法从这两个容器自动推断 data/rows 的实际类型，因此在文档生成阶段按
 * operationId 补全响应模型，不改变任何运行时返回值。</p>
 */
@Configuration
@SuppressWarnings({"rawtypes", "unchecked"})
public class H5OpenApiConfig {

    private static final String USER_CODES = """
            - `200`：成功
            - `601`：账号或密码错误，或用户不存在
            - `602`：原登录密码错误
            - `603`：用户名格式错误
            - `604`：登录密码格式错误
            - `605`：交易密码格式错误
            - `606`：手机号格式错误
            - `607`：性别参数错误
            - `608`：邀请码格式错误
            - `609`：用户名已存在
            - `610`：邀请码不存在
            - `611`：密码未填写
            - `612`：密码长度不合法，或新旧密码相同
            - `613`：交易密码错误
            - `614`：头像地址无效，或用户不存在
            - `617`：请求体无效或未知错误
            - `621`：手机号已存在
            - `622`：尝试次数过多，账号或密码校验暂时锁定
            - `703`：图片上传失败
            """;

    private static final String CONFIG_CODES = """
            - `200`：成功
            - `400`：分页或请求参数无效
            - `404`：站内信不存在
            - `500`：配置不可用或服务器内部错误
            - `701`：未查询到数据
            - `703`：图片上传失败
            """;

    private static final String ACCOUNT_CODES = """
            - `200`：成功
            - `500`：服务器内部错误
            - `501`：当前时间不在提现时段
            - `504`：交易密码缺失或错误
            - `505`：尚未完成提现所需任务
            - `506`：已有待审核的提现申请
            - `507`：余额不足
            - `509`：用户不存在
            - `510`：会员等级未配置
            - `513`：提现账户缺失、无效或不存在
            - `514`：提现功能或提现账户修改功能已禁用
            - `515`：请求参数、分页参数或状态参数无效
            - `516`：提现金额低于最低限额
            - `517`：提现金额高于最高限额
            - `518`：提现账户正在被使用，或发生并发修改冲突
            - `519`：平台当日提现总额已达上限
            - `521`：信用分未达到提现要求
            - `522`：提现后余额将低于会员等级最低余额
            - `523`：当日提现次数已达上限
            - `524`：当日提现金额已达上限
            - `525`：幂等键已被其他参数使用
            - `526`：交易密码授权凭证缺失、无效或已过期
            """;

    private static final String ORDER_CODES = """
            - `200`：成功
            - `500`：服务器内部错误
            - `901`：系统交易配置不可用
            - `902`：当前时间不在交易时段
            - `904`：用户不存在
            - `905`：当前账号状态不允许接单
            - `906`：余额低于接单最低要求
            - `908`：系统繁忙或并发冲突，请稍后重试
            - `910`：交易或订单配置无效
            - `911`：没有可匹配的商品
            - `912`：会员等级配置无效
            - `913`：订单不存在或不属于当前用户
            - `914`：用户状态无效
            - `916`：余额不足
            - `918`：订单状态冲突
            - `921`：彩金不存在或不属于当前用户
            - `922`：彩金已过期、已领取或不可领取
            - `923`：请求参数无效
            """;

    private static final String GOODS_CODES = """
            - `200`：成功；没有商品时 data/rows 返回空数组
            """;

    private static final Set<String> PUBLIC_OPERATIONS = Set.of(
            "login", "register", "logout",
            "errorMessages", "content", "website", "customerServices", "timeZone");

    private static final Map<String, ResponseSpec> RESPONSE_SPECS = responseSpecs();

    private static final Map<String, String> PARAMETER_DESCRIPTIONS = Map.ofEntries(
            Map.entry("lang", "语言代码，例如 zh_CN、zh_TW、en_US；不传时读取 Accept-Language 请求头"),
            Map.entry("pageNum", "页码，从 1 开始"),
            Map.entry("pageSize", "每页条数，范围 1～100"),
            Map.entry("status", "状态筛选；订单：0 已完成、1 待提交、2 冻结（3 已取消仅会出现在不传状态的全量列表中，当前筛选参数不接受 3）；提现：1 待审核、2 已通过、3 已驳回"),
            Map.entry("token", "交易密码校验后签发的一次性提现账户访问凭证"),
            Map.entry("Idempotency-Key", "创建订单幂等键，长度 8～64，仅允许字母、数字及 . _ : -"),
            Map.entry("file", "待上传的图片文件"),
            Map.entry("id", "资源 ID，具体资源类型见接口名称"));

    private static final Map<String, String> MODEL_DESCRIPTIONS = Map.ofEntries(
            Map.entry("UserLoginResult", "H5 登录结果"),
            Map.entry("LoginUserDto", "H5 登录请求"),
            Map.entry("RegisterDto", "H5 会员注册请求"),
            Map.entry("AvatarDto", "会员头像更新请求"),
            Map.entry("EditPasswordDto", "登录密码修改请求"),
            Map.entry("EditTradePasswordDto", "交易密码修改请求"),
            Map.entry("CheckTradePassword", "交易密码校验请求"),
            Map.entry("UserProfileResponse", "H5 当前会员资料"),
            Map.entry("MemberLevelResponse", "会员等级配置"),
            Map.entry("GlobalContentResponse", "当前语言的公共内容"),
            Map.entry("CustomerServiceResponse", "客服入口"),
            Map.entry("NoticeSummaryResponse", "公告或站内信摘要"),
            Map.entry("NoticeDetailResponse", "公告或站内信详情"),
            Map.entry("TimeZoneResponse", "系统时区配置"),
            Map.entry("TradeConfigResponse", "H5 交易与提现配置"),
            Map.entry("WebsiteConfigResponse", "H5 网站公开展示配置"),
            Map.entry("AvatarUploadResponse", "头像上传结果"),
            Map.entry("WithdrawalTypeResponse", "提现方式"),
            Map.entry("WithdrawalAccountRequest", "新增或修改提现账户的请求"),
            Map.entry("WithdrawalAccountResponse", "会员提现账户"),
            Map.entry("WithdrawalRequest", "发起提现请求"),
            Map.entry("WithdrawalResponse", "提现申请结果"),
            Map.entry("WithdrawalHistoryResponse", "提现历史记录"),
            Map.entry("DepositResponse", "充值记录"),
            Map.entry("TransactionResponse", "资金流水"),
            Map.entry("OrderResponse", "H5 订单"),
            Map.entry("BonusResponse", "待领取彩金"),
            Map.entry("SubmitResponse", "订单提交结果"),
            Map.entry("Goods", "H5 商品信息"));

    private static final Map<String, String> PROPERTY_DESCRIPTIONS = propertyDescriptions();

    @Bean
    public GlobalOpenApiCustomizer h5OpenApiCustomizer() {
        return H5OpenApiConfig::customize;
    }

    static Set<String> documentedOperationIds() {
        return Collections.unmodifiableSet(RESPONSE_SPECS.keySet());
    }

    static void customize(OpenAPI openApi) {
        if (openApi.getComponents() == null) {
            openApi.setComponents(new Components());
        }
        if (openApi.getComponents().getSchemas() == null) {
            openApi.getComponents().setSchemas(new LinkedHashMap<>());
        }

        addTagDescriptions(openApi);
        Paths paths = openApi.getPaths();
        if (paths == null) {
            return;
        }

        paths.forEach((path, pathItem) -> {
            if (!path.startsWith("/api/")) {
                return;
            }
            pathItem.readOperationsMap().forEach((method, operation) ->
                    customizeOperation(openApi, path, method, operation));
        });
        describeComponentSchemas(openApi);
        paths.forEach((path, pathItem) -> {
            if (!path.startsWith("/api/")) {
                return;
            }
            pathItem.readOperationsMap().forEach((method, operation) ->
                    describeOperation(openApi, path, method, operation));
        });
    }

    private static void customizeOperation(
            OpenAPI openApi,
            String path,
            PathItem.HttpMethod method,
            Operation operation) {
        String operationId = operation.getOperationId();
        ResponseSpec spec = RESPONSE_SPECS.get(operationId);
        if (spec == null) {
            return;
        }

        if (PUBLIC_OPERATIONS.contains(operationId)) {
            operation.setSecurity(Collections.emptyList());
        }
        removeServerParameters(operation);
        describeParameters(operation, operationId);

        String summary = operation.getSummary() == null ? operationId : operation.getSummary();
        ApiResponses responses = new ApiResponses();
        if (spec.shape() == ResponseShape.NO_CONTENT) {
            responses.addApiResponse("204", new ApiResponse().description("删除成功，无响应体"));
        } else {
            Schema<?> successSchema = responseSchema(openApi, spec);
            responses.addApiResponse(
                    spec.successStatus(),
                    jsonResponse("成功响应；业务是否成功仍以响应体 code 为准", successSchema));
        }

        if (!PUBLIC_OPERATIONS.contains(operationId)) {
            responses.addApiResponse("401", jsonResponse(
                    "未登录、Token 无效或 Token 已过期；响应体 code=401",
                    errorSchema("401=未授权")));
        }
        addRestErrorResponses(responses, spec);
        operation.setResponses(responses);

        if (summary.startsWith("兼容：") || Boolean.TRUE.equals(operation.getDeprecated())) {
            operation.setDeprecated(true);
        }
    }

    private static void describeOperation(
            OpenAPI openApi,
            String path,
            PathItem.HttpMethod method,
            Operation operation) {
        ResponseSpec spec = RESPONSE_SPECS.get(operation.getOperationId());
        if (spec == null) {
            return;
        }

        operation.setDescription(spec.detail()
                + "\n\n**请求方法与路径**：`" + method + " " + path + "`"
                + "\n\n**成功响应字段说明**\n\n"
                + inlineResponseFields(openApi, operation, spec)
                + "\n\n**响应 `code` 说明**\n" + codeDescription(spec.codeGroup()));
    }

    private static String inlineResponseFields(
            OpenAPI openApi,
            Operation operation,
            ResponseSpec spec) {
        if (spec.shape() == ResponseShape.NO_CONTENT) {
            return "- HTTP `204`：操作成功，无响应体字段。";
        }
        if (operation.getResponses() == null) {
            return "- 未声明成功响应字段。";
        }
        ApiResponse response = operation.getResponses().get(spec.successStatus());
        if (response == null || response.getContent() == null
                || response.getContent().get(org.springframework.http.MediaType.APPLICATION_JSON_VALUE) == null) {
            return "- 未声明成功响应字段。";
        }
        Schema<?> schema = response.getContent()
                .get(org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
                .getSchema();
        List<ResponseField> fields = new ArrayList<>();
        collectResponseFields(openApi, schema, "", fields, new LinkedHashSet<>(), 0);
        if (fields.isEmpty()) {
            return "- 未声明成功响应字段。";
        }

        StringBuilder documentation = new StringBuilder();
        for (ResponseField field : fields) {
            if (!documentation.isEmpty()) {
                documentation.append('\n');
            }
            documentation.append("- `")
                    .append(field.path())
                    .append("` (`")
                    .append(field.type())
                    .append("`)：")
                    .append(markdownText(field.description()));
        }
        return documentation.toString();
    }

    private static void collectResponseFields(
            OpenAPI openApi,
            Schema<?> schema,
            String path,
            List<ResponseField> fields,
            Set<String> visitingRefs,
            int depth) {
        if (schema == null || depth > 12) {
            return;
        }

        if (schema.get$ref() != null) {
            String schemaName = referencedSchemaName(schema);
            Schema<?> referenced = openApi.getComponents().getSchemas().get(schemaName);
            if (referenced != null && visitingRefs.add(schemaName)) {
                collectResponseFields(openApi, referenced, path, fields, visitingRefs, depth + 1);
                visitingRefs.remove(schemaName);
            }
            return;
        }

        if (schema.getProperties() != null) {
            ((Map<String, Schema<?>>) (Map<?, ?>) schema.getProperties()).forEach((name, property) -> {
                String fieldPath = path.isBlank() ? name : path + "." + name;
                addResponseField(fields, new ResponseField(
                        fieldPath,
                        schemaType(property),
                        inlineFieldDescription(openApi, fieldPath, property)));
                collectResponseFields(openApi, property, fieldPath, fields, visitingRefs, depth + 1);
            });
        }

        if (schema.getItems() != null) {
            collectResponseFields(openApi, schema.getItems(), path + "[]", fields, visitingRefs, depth + 1);
        }

        Object additionalProperties = schema.getAdditionalProperties();
        if (additionalProperties instanceof Schema<?> additionalSchema) {
            String fieldPath = path.isBlank() ? "{key}" : path + ".{key}";
            addResponseField(fields, new ResponseField(
                    fieldPath,
                    schemaType(additionalSchema),
                    inlineFieldDescription(openApi, fieldPath, additionalSchema)));
            collectResponseFields(openApi, additionalSchema, fieldPath, fields, visitingRefs, depth + 1);
        }

        collectComposedResponseFields(
                openApi, schema.getAllOf(), path, fields, visitingRefs, depth, false);
        collectComposedResponseFields(
                openApi, schema.getOneOf(), path, fields, visitingRefs, depth, true);
        collectComposedResponseFields(
                openApi, schema.getAnyOf(), path, fields, visitingRefs, depth, true);
    }

    private static void collectComposedResponseFields(
            OpenAPI openApi,
            List<Schema> schemas,
            String path,
            List<ResponseField> fields,
            Set<String> visitingRefs,
            int depth,
            boolean labelBranch) {
        if (schemas == null) {
            return;
        }
        for (Schema<?> child : schemas) {
            String childPath = labelBranch ? responseBranchPath(path, child) : path;
            collectResponseFields(openApi, child, childPath, fields, visitingRefs, depth + 1);
        }
    }

    private static void addResponseField(List<ResponseField> fields, ResponseField candidate) {
        boolean duplicate = fields.stream().anyMatch(field -> field.path().equals(candidate.path()));
        if (!duplicate) {
            fields.add(candidate);
        }
    }

    private static String responseBranchPath(String path, Schema<?> schema) {
        String schemaName = referencedSchemaName(schema);
        String label = switch (schemaName) {
            case "OrderResponse" -> "ORDER";
            case "BonusResponse" -> "BONUS";
            default -> schemaName.isBlank() ? "分支" : schemaName;
        };
        return path.isBlank() ? "[" + label + "]" : path + "[" + label + "]";
    }

    private static String referencedSchemaName(Schema<?> schema) {
        if (schema == null || schema.get$ref() == null) {
            return "";
        }
        String reference = schema.get$ref();
        return reference.substring(reference.lastIndexOf('/') + 1);
    }

    private static String inlineFieldDescription(
            OpenAPI openApi,
            String path,
            Schema<?> schema) {
        if ("code".equals(path)) {
            return "业务状态码；200 表示成功，其他取值见下方响应 code 说明";
        }
        String description = schema.getDescription();
        if ((description == null || description.isBlank()) && schema.get$ref() != null) {
            Schema<?> referenced = openApi.getComponents().getSchemas().get(referencedSchemaName(schema));
            if (referenced != null) {
                description = referenced.getDescription();
            }
        }
        if (description == null || description.isBlank()) {
            return path.endsWith(".{key}") || "{key}".equals(path)
                    ? "动态键对应的业务值"
                    : "该字段的业务值";
        }
        return description;
    }

    private static String schemaType(Schema<?> schema) {
        if (schema == null) {
            return "unknown";
        }
        if (schema.get$ref() != null) {
            return referencedSchemaName(schema);
        }
        if (schema.getItems() != null || "array".equals(schema.getType())) {
            return "array<" + schemaType(schema.getItems()) + ">";
        }
        List<Schema> variants = schema.getOneOf() != null && !schema.getOneOf().isEmpty()
                ? schema.getOneOf()
                : schema.getAnyOf();
        if (variants != null && !variants.isEmpty()) {
            return "oneOf<" + variants.stream()
                    .map(H5OpenApiConfig::schemaType)
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("object") + ">";
        }
        if (schema.getAllOf() != null && !schema.getAllOf().isEmpty()) {
            return schema.getAllOf().size() == 1
                    ? schemaType(schema.getAllOf().get(0))
                    : "object";
        }
        if (schema.getAdditionalProperties() instanceof Schema<?> additionalSchema) {
            return "map<string, " + schemaType(additionalSchema) + ">";
        }
        String type = schema.getType();
        if (type == null || type.isBlank()) {
            type = schema.getProperties() == null ? "object" : "object";
        }
        return schema.getFormat() == null || schema.getFormat().isBlank()
                ? type
                : type + "(" + schema.getFormat() + ")";
    }

    private static String markdownText(String value) {
        return value.replace("\r\n", "<br>")
                .replace("\r", "<br>")
                .replace("\n", "<br>");
    }

    private static void addRestErrorResponses(ApiResponses responses, ResponseSpec spec) {
        if (spec.codeGroup() == CodeGroup.CONFIG) {
            responses.addApiResponse("400", jsonResponse("请求参数错误", errorSchema(CONFIG_CODES)));
            responses.addApiResponse("404", jsonResponse("资源不存在", errorSchema(CONFIG_CODES)));
            responses.addApiResponse("500", jsonResponse("配置不可用或服务器内部错误", errorSchema(CONFIG_CODES)));
            addMethodNotAllowedResponse(responses);
            return;
        }
        if (spec.codeGroup() == CodeGroup.USER || spec.codeGroup() == CodeGroup.GOODS) {
            responses.addApiResponse("400", jsonResponse(
                    "请求格式或参数绑定无效；兼容接口也可能以 HTTP 200 返回业务错误码",
                    errorSchema(codeDescription(spec.codeGroup()))));
            addMethodNotAllowedResponse(responses);
            responses.addApiResponse("500", jsonResponse(
                    "服务器内部错误；响应体使用固定英文安全提示",
                    errorSchema("500=服务器内部错误")));
            return;
        }
        responses.addApiResponse("400", jsonResponse("请求参数或业务规则校验失败", errorSchema(codeDescription(spec.codeGroup()))));
        responses.addApiResponse("403", jsonResponse("当前用户或当前状态不允许执行该操作", errorSchema(codeDescription(spec.codeGroup()))));
        responses.addApiResponse("404", jsonResponse("用户、订单、彩金、提现记录或提现账户不存在", errorSchema(codeDescription(spec.codeGroup()))));
        responses.addApiResponse("409", jsonResponse("幂等、状态或并发修改冲突", errorSchema(codeDescription(spec.codeGroup()))));
        responses.addApiResponse("500", jsonResponse("服务器内部错误；响应体 code=500", errorSchema("500=服务器内部错误")));
        addMethodNotAllowedResponse(responses);
    }

    private static void addMethodNotAllowedResponse(ApiResponses responses) {
        responses.addApiResponse("405", jsonResponse(
                "请求方法不受支持；响应体使用固定英文安全提示",
                errorSchema("405=请求方法不受支持")));
    }

    private static Schema<?> responseSchema(OpenAPI openApi, ResponseSpec spec) {
        return switch (spec.shape()) {
            case AJAX -> ajaxSchema(openApi, spec);
            case PAGE -> pageSchema(openApi, spec);
            case ORDER_CREATION -> orderCreationSchema(openApi, spec);
            case UPLOAD -> uploadSchema(spec);
            case NO_CONTENT -> null;
        };
    }

    private static Schema<?> ajaxSchema(OpenAPI openApi, ResponseSpec spec) {
        ObjectSchema schema = baseResponse(spec);
        if (spec.dataType() != Void.class) {
            schema.addProperty("data", describedSchema(
                    dataSchema(openApi, spec.dataType(), spec.dataArray()),
                    spec.dataArray() ? "业务数据列表" : "业务数据"));
        }
        return schema;
    }

    private static Schema<?> pageSchema(OpenAPI openApi, ResponseSpec spec) {
        ObjectSchema schema = baseResponse(spec);
        schema.addProperty("rows", dataSchema(openApi, spec.dataType(), true)
                .description("当前页数据列表"));
        schema.addProperty("total", new IntegerSchema()
                .format("int64")
                .description("符合条件的总记录数")
                .example(1L));
        schema.required(List.of("code", "msg", "rows", "total"));
        return schema;
    }

    private static Schema<?> orderCreationSchema(OpenAPI openApi, ResponseSpec spec) {
        ObjectSchema schema = baseResponse(spec);
        Schema<Object> data = new ObjectSchema()
                .description("创建出的订单，或当前待领取的彩金");
        data.setOneOf(List.of(
                componentSchema(openApi, OrderApiDtos.OrderResponse.class),
                componentSchema(openApi, OrderApiDtos.BonusResponse.class)));
        schema.addProperty("data", data);
        schema.addProperty("resultType", new StringSchema()
                .description("返回数据类型：ORDER=订单，BONUS=待领取彩金")
                ._enum(List.of("ORDER", "BONUS"))
                .example("ORDER"));
        schema.required(List.of("code", "msg", "data", "resultType"));
        return schema;
    }

    private static Schema<?> uploadSchema(ResponseSpec spec) {
        ObjectSchema schema = baseResponse(spec);
        schema.addProperty("fileName", new StringSchema()
                .description("服务器保存的相对文件路径"));
        schema.addProperty("url", new StringSchema()
                .format("uri")
                .description("可访问的完整文件 URL"));
        return schema;
    }

    private static ObjectSchema baseResponse(ResponseSpec spec) {
        ObjectSchema schema = new ObjectSchema();
        schema.setDescription("H5 接口响应");
        schema.addProperty("code", new IntegerSchema()
                .format("int32")
                .description("业务状态码。200 表示成功；其他取值如下：\n"
                        + codeDescription(spec.codeGroup()))
                .example(200));
        schema.addProperty("msg", new StringSchema()
                .description("固定英文安全提示，不含中文且不受 lang 或 Accept-Language 影响；同一 code 在不同安全处理路径下提示可能不同")
                .example("Success"));
        schema.required(new ArrayList<>(List.of("code", "msg")));
        return schema;
    }

    private static Schema<?> errorSchema(String codeDescription) {
        ObjectSchema schema = new ObjectSchema();
        schema.setDescription("H5 错误响应");
        schema.addProperty("code", new IntegerSchema()
                .description("业务状态码：\n" + codeDescription)
                .example(500));
        schema.addProperty("msg", new StringSchema()
                .description("固定英文错误提示，不含中文且不会回显异常、数据库或其他内部文本；同一 code 在不同安全处理路径下提示可能不同")
                .example("Please try again later"));
        schema.addProperty("message", new StringSchema()
                .description("认证拦截器兼容字段，固定英文且内容与 msg 相同")
                .example("Unauthorized"));
        schema.required(List.of("code"));
        return schema;
    }

    private static Schema<?> dataSchema(OpenAPI openApi, Class<?> type, boolean array) {
        Schema<?> item;
        if (type == Map.class) {
            item = new MapSchema()
                    .additionalProperties(new StringSchema()
                            .description("业务状态码对应的本地化提示文案"));
        } else if (type == String.class) {
            item = new StringSchema().description("字符串业务数据");
        } else if (type == Integer.class) {
            item = new IntegerSchema().format("int32").description("整数业务数据");
        } else {
            item = componentSchema(openApi, type);
        }
        return array ? new ArraySchema().items(item) : item;
    }

    private static Schema<?> componentSchema(OpenAPI openApi, Class<?> type) {
        ResolvedSchema resolved = ModelConverters.getInstance()
                .resolveAsResolvedSchema(new AnnotatedType(type).resolveAsRef(true));
        if (resolved.referencedSchemas != null) {
            resolved.referencedSchemas.forEach(openApi.getComponents()::addSchemas);
        }
        return resolved.schema == null ? new ObjectSchema() : resolved.schema;
    }

    private static Schema<?> describedSchema(Schema<?> schema, String description) {
        if (schema.get$ref() == null) {
            schema.setDescription(description);
            return schema;
        }
        ComposedSchema wrapper = new ComposedSchema();
        wrapper.setDescription(description);
        wrapper.addAllOfItem(schema);
        return wrapper;
    }

    private static ApiResponse jsonResponse(String description, Schema<?> schema) {
        io.swagger.v3.oas.models.media.MediaType mediaType =
                new io.swagger.v3.oas.models.media.MediaType().schema(schema);
        return new ApiResponse()
                .description(description)
                .content(new io.swagger.v3.oas.models.media.Content()
                        .addMediaType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE, mediaType));
    }

    private static void removeServerParameters(Operation operation) {
        if (operation.getParameters() == null) {
            return;
        }
        operation.setParameters(operation.getParameters().stream()
                .filter(parameter -> !"userId".equals(parameter.getName()))
                .toList());
    }

    private static void describeParameters(Operation operation, String operationId) {
        if (operation.getParameters() == null) {
            return;
        }
        operation.getParameters().forEach(parameter -> {
            String description = PARAMETER_DESCRIPTIONS.get(parameter.getName());
            if ("id".equals(parameter.getName())) {
                description = idDescription(operationId);
            }
            if (description != null) {
                parameter.setDescription(description);
            }
        });
    }

    private static String idDescription(String operationId) {
        return switch (operationId) {
            case "get" -> "站内信 ID，且必须属于当前会员";
            case "order", "submit" -> "订单 ID，且必须属于当前会员";
            case "withdrawalAccount", "updateWithdrawalAccount", "deleteWithdrawalAccount" ->
                    "提现账户 ID，且必须属于当前会员";
            default -> "资源 ID";
        };
    }

    private static void describeComponentSchemas(OpenAPI openApi) {
        Map<String, Schema> schemas = openApi.getComponents().getSchemas();
        if (schemas == null) {
            return;
        }
        schemas.forEach((schemaName, schema) -> {
            String modelDescription = MODEL_DESCRIPTIONS.get(schemaName);
            if (modelDescription != null) {
                schema.setDescription(modelDescription);
            } else if (schema.getDescription() == null || schema.getDescription().isBlank()) {
                schema.setDescription("H5 接口数据模型");
            }
            if (schema.getProperties() == null) {
                return;
            }
            schema.getProperties().replaceAll((propertyName, propertyValue) -> {
                String name = String.valueOf(propertyName);
                Schema<?> property = (Schema<?>) propertyValue;
                String description = property.getDescription();
                if (description == null || description.isBlank()) {
                    description = PROPERTY_DESCRIPTIONS.getOrDefault(
                            schemaName + "." + name,
                            PROPERTY_DESCRIPTIONS.getOrDefault(
                                    name, "H5 接口字段：" + name));
                }
                property = describedSchema(property, description);
                Object example = propertyExample(name);
                if (example != null && property.getExample() == null) {
                    property.setExample(example);
                }
                return property;
            });
        });
    }

    private static Object propertyExample(String propertyName) {
        return switch (propertyName) {
            case "pageNum" -> 1;
            case "pageSize" -> 20;
            case "username" -> "member001";
            case "phoneNumber" -> "+8613800000000";
            case "gender" -> "2";
            case "lang" -> "zh_CN";
            case "tag" -> "zh-CN";
            case "status" -> "1";
            case "type" -> "0";
            case "isDefault" -> true;
            case "orderNumber" -> "WD202607290001";
            default -> null;
        };
    }

    private static void addTagDescriptions(OpenAPI openApi) {
        Map<String, Tag> existing = new LinkedHashMap<>();
        if (openApi.getTags() != null) {
            openApi.getTags().forEach(tag -> existing.put(tag.getName(), tag));
        }
        putTag(existing, "用户管理", "H5 登录、注册、会员资料及密码管理。\n\n**业务状态码**\n" + USER_CODES);
        putTag(existing, "前台配置", "H5 多语言、公共内容、客服、公告和交易配置。\n\n**业务状态码**\n" + CONFIG_CODES);
        putTag(existing, "会员站内信", "当前会员的站内信列表与详情。\n\n**业务状态码**\n" + CONFIG_CODES);
        putTag(existing, "商品管理", "H5 商品查询。\n\n**业务状态码**\n" + GOODS_CODES);
        putTag(existing, "订单管理", "H5 接单、提交、订单历史与彩金领取。\n\n**业务状态码**\n" + ORDER_CODES);
        putTag(existing, "账户管理", "H5 提现账户、提现申请、充值记录和资金流水。\n\n**业务状态码**\n" + ACCOUNT_CODES);
        openApi.setTags(new ArrayList<>(existing.values()));
    }

    private static void putTag(Map<String, Tag> tags, String name, String description) {
        Tag tag = tags.computeIfAbsent(name, key -> new Tag().name(key));
        tag.setDescription(description);
    }

    private static String codeDescription(CodeGroup group) {
        return switch (group) {
            case USER -> USER_CODES;
            case CONFIG -> CONFIG_CODES;
            case GOODS -> GOODS_CODES;
            case ORDER -> ORDER_CODES;
            case ACCOUNT -> ACCOUNT_CODES;
        };
    }

    private static Map<String, ResponseSpec> responseSpecs() {
        Map<String, ResponseSpec> specs = new LinkedHashMap<>();

        // 用户
        add(specs, "login", ajax(UserLoginResult.class, false, CodeGroup.USER,
                "使用用户名和登录密码登录，成功后返回 Bearer Token 与当前会员资料。"));
        add(specs, "register", ajax(Void.class, false, CodeGroup.USER,
                "注册 H5 会员账号；用户名、手机号和邀请码会进行唯一性与有效性校验。"));
        add(specs, "logout", ajax(Void.class, false, CodeGroup.USER,
                "注销当前 Token。该操作具备幂等性，没有有效 Token 时也返回成功。"));
        add(specs, "userInfo", ajax(UserProfileResponse.class, false, CodeGroup.USER,
                "查询当前登录会员的公开资料、余额、任务进度和会员等级。"));
        add(specs, "uploadAvatar", ajax(ConfigApiDtos.AvatarUploadResponse.class, false, CodeGroup.USER,
                "上传图片并立即更新当前会员头像；请求类型为 multipart/form-data。"));
        add(specs, "editPassword", ajax(Void.class, false, CodeGroup.USER,
                "校验原登录密码后设置新密码，并使该会员已有 Token 失效。"));
        add(specs, "editTradePassword", ajax(Void.class, false, CodeGroup.USER,
                "校验原交易密码后设置新的交易密码。"));
        add(specs, "checkTradePassword", ajax(String.class, false, CodeGroup.USER,
                "校验交易密码；成功后 data 返回一次性提现账户访问凭证。"));

        // 商品
        add(specs, "getGoodsList", ajax(Goods.class, true, CodeGroup.GOODS,
                "随机获取可供 H5 展示或接单使用的商品列表。"));

        // 订单
        add(specs, "create", creation(
                "使用 Idempotency-Key 幂等创建订单；如当前有待领取彩金，则返回彩金而不是新订单。"));
        add(specs, "submit", ajax(OrderApiDtos.SubmitResponse.class, false, CodeGroup.ORDER,
                "提交当前会员名下的待处理订单；重复提交已完成订单时返回幂等结果。"));
        add(specs, "order", ajax(OrderApiDtos.OrderResponse.class, false, CodeGroup.ORDER,
                "查询当前会员名下的指定订单详情。"));
        add(specs, "orders", page(OrderApiDtos.OrderResponse.class, CodeGroup.ORDER,
                "分页查询当前会员的订单，可按订单状态筛选。"));

        // 前台配置
        add(specs, "errorMessages", ajax(Map.class, false, CodeGroup.CONFIG,
                "获取当前语言下的业务状态码提示文案，data 的键为 code 字符串。"));
        add(specs, "content", ajax(ConfigApiDtos.GlobalContentResponse.class, false, CodeGroup.CONFIG,
                "获取当前语言下的协议、帮助、条款和业务说明内容。"));
        add(specs, "website", ajax(ConfigApiDtos.WebsiteConfigResponse.class, false, CodeGroup.CONFIG,
                "获取公开 H5 网站名称、货币单位、Logo、背景图及开屏弹窗等展示配置。"));
        add(specs, "customerServices", ajax(ConfigApiDtos.CustomerServiceResponse.class, true, CodeGroup.CONFIG,
                "获取当前语言下已启用的客服入口。"));
        add(specs, "memberLevels", ajax(ConfigApiDtos.MemberLevelResponse.class, true, CodeGroup.CONFIG,
                "获取当前语言下的 H5 会员等级、佣金和提现限制配置。"));
        add(specs, "timeZone", ajax(ConfigApiDtos.TimeZoneResponse.class, false, CodeGroup.CONFIG,
                "获取业务时间计算使用的系统时区。"));
        add(specs, "trade", ajax(ConfigApiDtos.TradeConfigResponse.class, false, CodeGroup.CONFIG,
                "获取 H5 接单、订单过期和提现限制等交易配置。"));
        add(specs, "upload", upload(
                "上传提现账户附件图片；请求类型为 multipart/form-data。"));

        // 站内信
        add(specs, "list", page(ConfigApiDtos.NoticeSummaryResponse.class, CodeGroup.CONFIG,
                "分页查询只属于当前会员的站内信摘要。"));
        add(specs, "get", ajax(ConfigApiDtos.NoticeDetailResponse.class, false, CodeGroup.CONFIG,
                "查询只属于当前会员的站内信详情。"));

        // 提现账户与资金记录
        add(specs, "withdrawalTypes", ajax(AccountApiDtos.WithdrawalTypeResponse.class, true, CodeGroup.ACCOUNT,
                "获取 H5 当前可选的银行卡或数字钱包提现方式。"));
        add(specs, "withdrawalAccounts", ajax(AccountApiDtos.WithdrawalAccountResponse.class, true, CodeGroup.ACCOUNT,
                "获取当前会员的有效提现账户；敏感账号默认以掩码形式返回。"));
        add(specs, "createWithdrawalAccount", createdAjax(AccountApiDtos.WithdrawalAccountResponse.class, CodeGroup.ACCOUNT,
                "使用交易密码授权凭证新增当前会员的提现账户。"));
        add(specs, "withdrawalAccount", ajax(AccountApiDtos.WithdrawalAccountResponse.class, false, CodeGroup.ACCOUNT,
                "使用交易密码授权凭证获取当前会员的提现账户编辑详情。"));
        add(specs, "updateWithdrawalAccount", ajax(AccountApiDtos.WithdrawalAccountResponse.class, false, CodeGroup.ACCOUNT,
                "使用交易密码授权凭证修改当前会员的提现账户。"));
        add(specs, "deleteWithdrawalAccount", noContent(CodeGroup.ACCOUNT,
                "使用交易密码授权凭证软删除当前会员的提现账户。"));
        add(specs, "withdraw", createdAjax(AccountApiDtos.WithdrawalResponse.class, CodeGroup.ACCOUNT,
                "使用 requestId 幂等发起提现，并校验交易密码、余额、任务、等级及当日限额。"));
        add(specs, "withdrawals", page(AccountApiDtos.WithdrawalHistoryResponse.class, CodeGroup.ACCOUNT,
                "分页查询当前会员的提现记录，可按审核状态筛选。"));
        add(specs, "deposits", page(AccountApiDtos.DepositResponse.class, CodeGroup.ACCOUNT,
                "分页查询当前会员的充值记录。"));
        add(specs, "transactions", page(AccountApiDtos.TransactionResponse.class, CodeGroup.ACCOUNT,
                "分页查询当前会员的资金变动流水。"));
        return Collections.unmodifiableMap(specs);
    }

    private static Map<String, String> propertyDescriptions() {
        Map<String, String> descriptions = new LinkedHashMap<>();
        descriptions.put("id", "记录主键 ID");
        descriptions.put("userId", "会员 ID");
        descriptions.put("username", "会员用户名");
        descriptions.put("password", "登录密码；仅用于请求，不会在响应中返回");
        descriptions.put("oldPassword", "当前登录密码");
        descriptions.put("newPassword", "新的登录密码，长度 6～64");
        descriptions.put("tradePassword", "交易密码；仅用于请求，不会在响应中返回");
        descriptions.put("oldTradePassword", "当前交易密码");
        descriptions.put("newTradePassword", "新的交易密码，长度 6～18");
        descriptions.put("phoneNumber", "E.164 格式手机号");
        descriptions.put("avatar", "头像相对路径或完整 URL");
        descriptions.put("gender", "性别：0 未知，1 男，2 女");
        descriptions.put("email", "电子邮箱");
        descriptions.put("birthday", "生日，时间戳格式");
        descriptions.put("vipId", "当前会员等级 ID");
        descriptions.put("parentId", "上级会员 ID");
        descriptions.put("parentUsername", "上级会员用户名");
        descriptions.put("parentInviteCode", "上级会员邀请码");
        descriptions.put("inviteCode", "注册时为邀请人邀请码；会员资料中为当前会员邀请码");
        descriptions.put("token", "H5 Bearer Token；交易密码校验接口中为一次性访问凭证");
        descriptions.put("user", "登录会员资料");
        descriptions.put("balance", "可用余额");
        descriptions.put("frozenBalance", "冻结余额");
        descriptions.put("baseSalary", "基础薪资金额");
        descriptions.put("taskProgress", "当前任务完成进度");
        descriptions.put("reputationScore", "会员信用分");
        descriptions.put("todayCommission", "今日累计佣金");
        descriptions.put("workLimit", "可接任务或工作额度");
        descriptions.put("isEnabled", "启用状态：0 启用，1 停用");
        descriptions.put("allowInvite", "是否允许邀请新会员");
        descriptions.put("isFrozen", "资金是否冻结");
        descriptions.put("isBanned", "账号是否封禁");
        descriptions.put("isWithdrawalNotification", "是否开启提现通知");
        descriptions.put("productMatching", "商品匹配开关");
        descriptions.put("accountStatus", "账号状态代码");
        descriptions.put("transactionStatus", "交易功能状态代码");
        descriptions.put("withdrawalStatus", "提现功能状态代码");
        descriptions.put("depositBlockWithdrawal", "充值后是否限制提现");
        descriptions.put("assistWithdrawalStatus", "人工协助提现状态");
        descriptions.put("memberLevel", "当前会员等级详情");

        descriptions.put("code", "语言代码，例如 zh_CN");
        descriptions.put("tag", "标准 IETF 语言标签，例如 zh-CN");
        descriptions.put("label", "语言的本地化显示名称");
        descriptions.put("h5Enabled", "该语言是否允许在 H5 使用");
        descriptions.put("lang", "本次返回内容使用的语言代码");
        descriptions.put("protocolContent", "用户协议内容");
        descriptions.put("aboutContent", "关于我们内容");
        descriptions.put("certificateContent", "资质证书内容");
        descriptions.put("helpContent", "帮助中心内容");
        descriptions.put("termsContent", "条款与条件内容");
        descriptions.put("eventContent", "活动说明内容");
        descriptions.put("transactionDescription", "交易说明内容");
        descriptions.put("orderDescription", "订单说明内容");
        descriptions.put("usageDescription", "使用说明内容");
        descriptions.put("name", "名称");
        descriptions.put("currencyUnit", "网站展示使用的货币单位");
        descriptions.put("copyright", "网站版权声明");
        descriptions.put("logo", "网站 Logo 图片 URL 或相对路径");
        descriptions.put("popUpLimit", "开屏弹窗最多展示次数，0 表示不限制");
        descriptions.put("popUpImage", "开屏弹窗图片 URL 或相对路径");
        descriptions.put("backgroundImage", "网站桌面端背景图 URL 或相对路径");
        descriptions.put("h5BackgroundImage", "H5 背景图 URL 或相对路径");
        descriptions.put("showLogo", "是否显示 Logo：0 否，1 是");
        descriptions.put("hideImage", "是否隐藏 Logo 图片：0 否，1 是");
        descriptions.put("imageShowTimeRange", "Logo 图片显示时间范围");
        descriptions.put("sortOrder", "展示排序值，数值越小越靠前");
        descriptions.put("image", "图片 URL 或相对路径");
        descriptions.put("link", "跳转链接或客服会话地址");
        descriptions.put("noticeId", "公告或站内信 ID");
        descriptions.put("noticeTitle", "公告或站内信标题");
        descriptions.put("noticeContent", "公告或站内信正文");
        descriptions.put("tzName", "IANA 时区名称，例如 Asia/Shanghai");
        descriptions.put("registerBonusAmount", "注册赠送金额");
        descriptions.put("minTradeBalance", "开始接单所需最低余额");
        descriptions.put("memberWithdrawalStatus", "会员提现总开关");
        descriptions.put("minCreditScoreForWithdrawal", "允许提现的最低信用分");
        descriptions.put("minWithdrawalAmount", "平台最低提现金额");
        descriptions.put("maxWithdrawalAmount", "平台最高提现金额");
        descriptions.put("withdrawalFeeRate", "提现手续费百分比");
        descriptions.put("serviceTimeRange", "客服服务时间范围");
        descriptions.put("tradeTimeRange", "允许接单的时间范围");
        descriptions.put("withdrawalTimeRange", "允许提现的时间范围");
        descriptions.put("orderExpireSeconds", "订单有效期，单位秒");
        descriptions.put("startTaskDelayMs", "开始任务延迟，单位毫秒");
        descriptions.put("submitTaskDelayMs", "提交任务延迟，单位毫秒");
        descriptions.put("requiredTaskGroupsForWithdrawal", "提现前必须完成的任务组数量");
        descriptions.put("allowModifyWithdrawalAddress", "是否允许会员修改提现账户");
        descriptions.put("fileName", "服务器保存的相对文件路径");
        descriptions.put("url", "文件完整访问 URL");

        descriptions.put("level", "会员等级序号");
        descriptions.put("icon", "会员等级图标 URL");
        descriptions.put("price", "等级价格或商品价格");
        descriptions.put("minBalance", "该等级要求的最低余额");
        descriptions.put("inviteCount", "该等级要求或允许的邀请人数");
        descriptions.put("orderCountPerDay", "每日订单数量");
        descriptions.put("minCommissionRate", "最低佣金比例");
        descriptions.put("maxCommissionRate", "最高佣金比例");
        descriptions.put("minContinuousCommissionRate", "连续任务最低佣金比例");
        descriptions.put("maxContinuousCommissionRate", "连续任务最高佣金比例");
        descriptions.put("taskCountPerDay", "每日任务数量");
        descriptions.put("withdrawCountPerDay", "每日可提现次数");
        descriptions.put("withdrawFeeRate", "会员等级提现手续费比例");
        descriptions.put("minWithdrawAmount", "会员等级最低提现金额");
        descriptions.put("withdrawLimitPerDay", "会员等级每日提现金额上限");
        descriptions.put("minWithdraw", "会员等级单笔最低提现金额");
        descriptions.put("maxWithdraw", "会员等级单笔最高提现金额");
        descriptions.put("description", "说明或详情");
        descriptions.put("productMatchEnabled", "该等级是否启用商品金额匹配");
        descriptions.put("productMatchMin", "商品匹配最低金额");
        descriptions.put("productMatchMax", "商品匹配最高金额");

        descriptions.put("withdrawalTypeId", "提现方式 ID");
        descriptions.put("typeName", "提现方式显示名称");
        descriptions.put("withdrawalTypeName", "提现方式名称");
        descriptions.put("withdrawalType", "提现方式名称或代码");
        descriptions.put("type", "类型代码；提现方式中 0=银行卡、1=数字钱包");
        descriptions.put("isDefault", "是否为默认提现账户");
        descriptions.put("bankName", "银行名称");
        descriptions.put("depositType", "账户或存款类型");
        descriptions.put("branchCode", "银行支行代码");
        descriptions.put("branchName", "银行支行名称");
        descriptions.put("bankAccount", "银行账号；列表响应通常为掩码值");
        descriptions.put("accountHolder", "银行账户持有人；列表响应通常为掩码值");
        descriptions.put("accountName", "账户名称；列表响应通常为掩码值");
        descriptions.put("walletName", "数字钱包名称或网络名称");
        descriptions.put("walletAddress", "数字钱包地址；列表响应通常为掩码值");
        descriptions.put("attachment", "钱包凭证图片 URL 或相对路径");
        descriptions.put("requestId", "提现请求幂等键 UUID");
        descriptions.put("withdrawalAccountId", "提现账户 ID");
        descriptions.put("walletId", "兼容字段：提现账户 ID");
        descriptions.put("withdrawalId", "提现记录 ID");
        descriptions.put("amount", "金额");
        descriptions.put("fee", "手续费金额");
        descriptions.put("netAmount", "扣除手续费后的预计到账金额");
        descriptions.put("status", "业务状态代码，具体取值以所属响应模型的字段说明为准");
        descriptions.put("remarks", "审核备注或业务备注");
        descriptions.put("account", "提现账户脱敏摘要");
        descriptions.put("giftAmount", "充值赠送金额");
        descriptions.put("receivedAmount", "实际到账金额");
        descriptions.put("transactionType", "资金变动类型");
        descriptions.put("serialCode", "资金流水编号");
        descriptions.put("transactionAmount", "本次资金变动金额，正数为增加、负数为减少");
        descriptions.put("balanceBefore", "变动前余额");
        descriptions.put("balanceAfter", "变动后余额");
        descriptions.put("transactionCode", "关联业务或交易编号");
        descriptions.put("remark", "备注");

        descriptions.put("orderNumber", "业务订单号");
        descriptions.put("orderCount", "当前订单在任务组中的序号");
        descriptions.put("rebatePercentage", "订单佣金比例");
        descriptions.put("rebate", "订单佣金金额");
        descriptions.put("expiryTime", "过期时间，Unix 毫秒时间戳");
        descriptions.put("productId", "商品 ID");
        descriptions.put("productImage", "商品图片 URL");
        descriptions.put("productTitle", "商品标题");
        descriptions.put("orderNum", "彩金对应的任务序号");
        descriptions.put("animationDuration", "彩金动画持续时间，单位秒");
        descriptions.put("displayDuration", "彩金弹窗展示时间，单位秒");
        descriptions.put("distributionType", "彩金发放类型代码");
        descriptions.put("alreadyClaimed", "本次请求前是否已经领取；用于表示幂等结果");
        descriptions.put("alreadyCompleted", "本次请求前订单是否已经完成；用于表示幂等结果");
        descriptions.put("resultType", "创建订单结果类型：ORDER=订单，BONUS=彩金");

        descriptions.put("title", "商品标题");
        descriptions.put("typeId", "商品类目 ID");
        descriptions.put("typeTitle", "商品类目名称");
        descriptions.put("serialNumber", "商品展示序号");
        descriptions.put("subTitle", "商品副标题");
        descriptions.put("unitPrice", "商品单价");
        descriptions.put("quantity", "商品数量");
        descriptions.put("starRating", "商品星级");
        descriptions.put("rating", "商品评分");

        descriptions.put("pageNum", "页码，从 1 开始");
        descriptions.put("pageSize", "每页条数，范围 1～100");
        descriptions.put("rows", "当前页数据列表");
        descriptions.put("total", "符合条件的总记录数");
        descriptions.put("createTime", "创建时间");
        descriptions.put("createdAt", "创建时间");
        descriptions.put("createdTime", "创建时间");
        descriptions.put("updateTime", "最后更新时间");
        descriptions.put("createBy", "创建人");
        descriptions.put("updateBy", "最后更新人");
        descriptions.put("searchValue", "兼容查询字段");
        descriptions.put("params", "兼容扩展参数");
        descriptions.put("deleted", "软删除状态：0 有效，1 已删除");
        descriptions.put("deletedTime", "软删除时间");
        descriptions.put("bankAccountMask", "银行账号掩码");
        descriptions.put("accountHolderMask", "账户持有人掩码");
        descriptions.put("accountNameMask", "账户名称掩码");
        descriptions.put("walletAddressMask", "钱包地址掩码");

        // 同名字段必须优先按所属模型解释，避免把订单类型、提现类型和不同状态机混为一谈。
        descriptions.put("UserProfileResponse.id", "当前会员 ID");
        descriptions.put("UserProfileResponse.luckyBonus", "累计已发放彩金金额；无已发放彩金时为 0");
        descriptions.put("CustomerServiceResponse.id", "客服入口 ID");
        descriptions.put("MemberLevelResponse.id", "会员等级 ID");
        descriptions.put("Goods.id", "商品 ID");
        descriptions.put("OrderResponse.id", "订单 ID");
        descriptions.put("BonusResponse.id", "彩金 ID");
        descriptions.put("SubmitResponse.id", "订单 ID");
        descriptions.put("WithdrawalTypeResponse.id", "提现方式 ID");
        descriptions.put("WithdrawalAccountResponse.id", "提现账户 ID");
        descriptions.put("WithdrawalHistoryResponse.id", "提现记录 ID");
        descriptions.put("DepositResponse.id", "充值记录 ID");
        descriptions.put("TransactionResponse.id", "资金流水 ID");

        descriptions.put("OrderResponse.type", "订单类型：0 普通订单，1 连单");
        descriptions.put("WithdrawalTypeResponse.type", "提现方式类别：0 银行卡，1 数字钱包");
        descriptions.put("WithdrawalAccountResponse.type", "提现账户类别：0 银行卡，1 数字钱包");

        descriptions.put("OrderResponse.status", "订单状态：0 已完成，1 待提交，2 冻结，3 已取消");
        descriptions.put("SubmitResponse.status", "订单提交后的状态：0 已完成，2 冻结");
        descriptions.put("WithdrawalResponse.status", "提现审核状态：1 待审核，2 已通过，3 已驳回");
        descriptions.put("WithdrawalHistoryResponse.status", "提现审核状态：1 待审核，2 已通过，3 已驳回");
        descriptions.put("DepositResponse.status", "充值审核状态：1 待审核，2 已通过，3 已驳回");

        descriptions.put("OrderResponse.amount", "订单本金金额");
        descriptions.put("BonusResponse.amount", "彩金金额");
        descriptions.put("WithdrawalResponse.amount", "提现申请金额");
        descriptions.put("WithdrawalHistoryResponse.amount", "提现申请金额");
        descriptions.put("DepositResponse.amount", "充值本金金额");
        descriptions.put("OrderResponse.orderNumber", "订单业务编号");
        descriptions.put("WithdrawalResponse.orderNumber", "提现业务编号");
        descriptions.put("WithdrawalHistoryResponse.orderNumber", "提现业务编号");
        descriptions.put("DepositResponse.orderNumber", "充值业务编号");

        descriptions.put("DepositResponse.transactionType", "充值记录资金类型代码，例如 ck 表示充值");
        descriptions.put("TransactionResponse.transactionType", "资金流水业务类型代码，例如 fy 表示佣金返还、zs 表示赠送");
        descriptions.put("DepositResponse.remark", "充值记录备注");
        descriptions.put("TransactionResponse.remark", "资金流水备注");

        descriptions.put("WebsiteConfigResponse.name", "网站展示名称");
        descriptions.put("CustomerServiceResponse.name", "客服入口显示名称");
        descriptions.put("MemberLevelResponse.name", "会员等级名称");
        descriptions.put("Goods.description", "商品说明或详情");
        descriptions.put("MemberLevelResponse.description", "会员等级说明");
        descriptions.put("Goods.isEnabled", "商品启用状态：0 启用，1 停用");
        descriptions.put("UserProfileResponse.isEnabled", "会员启用状态：0 启用，1 停用");

        descriptions.put("OrderResponse.expiryTime", "订单过期时间，Unix 毫秒时间戳");
        descriptions.put("BonusResponse.expiryTime", "彩金领取截止时间，Unix 毫秒时间戳");
        descriptions.put("BonusResponse.animationDuration", "彩金动画持续时间，单位秒");
        descriptions.put("BonusResponse.displayDuration", "彩金弹窗展示时间，单位秒");
        return Collections.unmodifiableMap(descriptions);
    }

    private static void add(Map<String, ResponseSpec> specs, String operationId, ResponseSpec spec) {
        if (specs.put(operationId, spec) != null) {
            throw new IllegalStateException("Duplicate H5 OpenAPI operationId: " + operationId);
        }
    }

    private static ResponseSpec ajax(
            Class<?> dataType, boolean array, CodeGroup group, String detail) {
        return new ResponseSpec(ResponseShape.AJAX, dataType, array, "200", group, detail);
    }

    private static ResponseSpec createdAjax(Class<?> dataType, CodeGroup group, String detail) {
        return new ResponseSpec(ResponseShape.AJAX, dataType, false, "201", group, detail);
    }

    private static ResponseSpec page(Class<?> rowType, CodeGroup group, String detail) {
        return new ResponseSpec(ResponseShape.PAGE, rowType, true, "200", group, detail);
    }

    private static ResponseSpec creation(String detail) {
        return new ResponseSpec(
                ResponseShape.ORDER_CREATION, Object.class, false, "200", CodeGroup.ORDER, detail);
    }

    private static ResponseSpec upload(String detail) {
        return new ResponseSpec(
                ResponseShape.UPLOAD, Void.class, false, "200", CodeGroup.CONFIG, detail);
    }

    private static ResponseSpec noContent(CodeGroup group, String detail) {
        return new ResponseSpec(ResponseShape.NO_CONTENT, Void.class, false, "204", group, detail);
    }

    private enum ResponseShape {
        AJAX,
        PAGE,
        ORDER_CREATION,
        UPLOAD,
        NO_CONTENT
    }

    private enum CodeGroup {
        USER,
        CONFIG,
        GOODS,
        ORDER,
        ACCOUNT
    }

    private record ResponseSpec(
            ResponseShape shape,
            Class<?> dataType,
            boolean dataArray,
            String successStatus,
            CodeGroup codeGroup,
            String detail) {
    }

    private record ResponseField(String path, String type, String description) {
    }
}
