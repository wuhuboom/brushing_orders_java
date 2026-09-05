package com.order.web.core.config;

import com.order.api.controller.AccountController;
import com.order.api.controller.AuthController;
import com.order.api.controller.ConfigController;
import com.order.api.controller.GoodsApiController;
import com.order.api.controller.OrderController;
import com.order.api.controller.SiteMessageController;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class H5OpenApiConfigTest {
    private static final Pattern HAN = Pattern.compile("\\p{IsHan}");
    private static final Pattern INLINE_RESPONSE_FIELD = Pattern.compile(
            "(?m)^- `([^`]+)` \\(`[^`]+`\\)：(.+)$");

    // Mirrors the calls exported by iron-h5/src/api/client.ts.
    private static final Set<String> CURRENT_H5_OPERATIONS = Set.of(
            "login", "register", "logout", "userInfo", "uploadAvatar",
            "editPassword", "editTradePassword", "checkTradePassword",
            "errorMessages", "content", "website", "customerServices",
            "memberLevels", "timeZone", "trade", "upload",
            "getGoodsList",
            "create", "submit", "order", "orders",
            "list", "get",
            "withdrawalTypes", "withdrawalAccounts", "createWithdrawalAccount",
            "withdrawalAccount", "updateWithdrawalAccount", "deleteWithdrawalAccount",
            "withdraw", "withdrawals", "deposits", "transactions");

    private static final List<Class<?>> H5_CONTROLLERS = List.of(
            AccountController.class,
            AuthController.class,
            ConfigController.class,
            GoodsApiController.class,
            OrderController.class,
            SiteMessageController.class);

    @Test
    void documentsEveryH5ControllerOperation() {
        Set<String> controllerOperations = new LinkedHashSet<>();
        for (Class<?> controller : H5_CONTROLLERS) {
            for (Method method : controller.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Operation.class)) {
                    controllerOperations.add(method.getName());
                }
            }
        }

        assertEquals(controllerOperations, H5OpenApiConfig.documentedOperationIds());
    }

    @Test
    void exposesOnlyOperationsUsedByTheCurrentH5Client() {
        assertEquals(CURRENT_H5_OPERATIONS, H5OpenApiConfig.documentedOperationIds());
    }

    @Test
    void addsChineseDescriptionsAndActualResponseModels() {
        OpenAPI openApi = controllerOpenApi();

        openApi.getPaths().values().forEach(pathItem -> {
            pathItem.readOperations().forEach(operation -> {
                assertNotNull(operation.getDescription());
                assertTrue(operation.getDescription().contains("响应 `code` 说明"));
                assertFalse(operation.getResponses().isEmpty());
            });
        });

        io.swagger.v3.oas.models.Operation login =
                openApi.getPaths().get("/api/user/login").getPost();
        Schema<?> loginSchema = login.getResponses().get("200")
                .getContent().get("application/json").getSchema();
        assertNotNull(loginSchema.getProperties().get("code").getDescription());
        assertTrue(loginSchema.getProperties().get("code").getDescription().contains("200"));
        assertNotNull(loginSchema.getProperties().get("data"));

        Schema<?> loginResult = openApi.getComponents().getSchemas().get("UserLoginResult");
        assertNotNull(loginResult);
        assertEquals("H5 Bearer Token；交易密码校验接口中为一次性访问凭证",
                loginResult.getProperties().get("token").getDescription());
        assertEquals("登录会员资料",
                loginResult.getProperties().get("user").getDescription());
        assertEquals(6, openApi.getTags().size());
    }

    @Test
    void everyPublicOperationAndResponseFieldHasMeaningfulChineseDocumentation() {
        OpenAPI openApi = controllerOpenApi();

        List<String> failures = new ArrayList<>();
        openApi.getPaths().forEach((path, item) -> item.readOperations().forEach(operation -> {
            if (!HAN.matcher(operation.getSummary()).find()) {
                failures.add(path + " summary");
            }
            if (operation.getDescription() == null
                    || !HAN.matcher(operation.getDescription()).find()) {
                failures.add(path + " description");
            }
            if (operation.getParameters() != null) {
                operation.getParameters().forEach(parameter -> {
                    if (parameter.getDescription() == null
                            || !HAN.matcher(parameter.getDescription()).find()) {
                        failures.add(path + " -> " + parameter.getName()
                                + " parameter description");
                    }
                });
            }
            if (operation.getRequestBody() != null
                    && operation.getRequestBody().getContent() != null) {
                operation.getRequestBody().getContent().forEach((mediaType, content) ->
                        inspectSchema(openApi, content.getSchema(),
                                path + " -> request body", failures, new HashSet<>()));
            }
            if (operation.getResponses() != null) {
                operation.getResponses().forEach((status, response) -> {
                    if (response.getContent() != null) {
                        response.getContent().forEach((mediaType, content) ->
                                inspectSchema(openApi, content.getSchema(),
                                        path + " -> " + status, failures, new HashSet<>()));
                    }
                });
            }
        }));

        assertTrue(failures.isEmpty(), () -> String.join("\n", failures));
    }

    @Test
    void showsCompleteSuccessResponseFieldsInsideEveryOperationDescription() {
        OpenAPI openApi = controllerOpenApi();
        List<String> failures = new ArrayList<>();

        openApi.getPaths().forEach((path, item) -> item.readOperations().forEach(operation -> {
            String description = operation.getDescription();
            if (description == null || !description.contains("**成功响应字段说明**")) {
                failures.add(path + " missing inline response field section");
                return;
            }
            if ("deleteWithdrawalAccount".equals(operation.getOperationId())) {
                if (!description.contains("HTTP `204`：操作成功，无响应体字段")) {
                    failures.add(path + " missing 204 no-content documentation");
                }
                return;
            }
            Schema<?> successSchema = successSchema(operation);
            Set<String> expectedPaths = new LinkedHashSet<>();
            collectInlineFieldPaths(
                    openApi, successSchema, "", expectedPaths, new HashSet<>(), 0);
            Set<String> documentedPaths = new LinkedHashSet<>();
            Matcher matcher = INLINE_RESPONSE_FIELD.matcher(description);
            while (matcher.find()) {
                documentedPaths.add(matcher.group(1));
                if (!HAN.matcher(matcher.group(2)).find()) {
                    failures.add(path + " -> " + matcher.group(1)
                            + " inline description is not meaningful Chinese");
                }
            }
            if (!expectedPaths.equals(documentedPaths)) {
                failures.add(path + " inline field paths differ from success schema; expected="
                        + expectedPaths + ", documented=" + documentedPaths);
            }
            if (!description.contains("- `code` (`integer(int32)`)：")
                    || !description.contains("- `msg` (`string`)：")) {
                failures.add(path + " missing code/msg inline documentation");
            }
            if (description.contains("未声明成功响应字段")
                    || description.contains("业务响应字段")
                    || description.contains("该字段的业务值")
                    || description.contains("H5 接口字段：")) {
                failures.add(path + " contains placeholder inline documentation:\n" + description);
            }
        }));

        assertTrue(failures.isEmpty(), () -> String.join("\n", failures));

        String content = description(openApi, "/api/config/content", PathItem.HttpMethod.GET);
        assertInlineFields(content,
                "data.lang",
                "data.protocolContent",
                "data.aboutContent",
                "data.certificateContent",
                "data.helpContent",
                "data.termsContent",
                "data.eventContent",
                "data.transactionDescription",
                "data.orderDescription",
                "data.usageDescription");

        String levels = description(openApi, "/api/config/member-levels", PathItem.HttpMethod.GET);
        assertInlineFields(levels,
                "data[].id",
                "data[].name",
                "data[].minCommissionRate",
                "data[].description",
                "data[].withdrawFeeRate");

        String orders = description(openApi, "/api/order", PathItem.HttpMethod.GET);
        assertInlineFields(orders, "rows", "rows[].id", "rows[].status", "total");

        String order = description(openApi, "/api/order/{id}", PathItem.HttpMethod.GET);
        assertInlineFields(order, "data.id", "data.orderNumber", "data.status");

        String creation = description(openApi, "/api/order", PathItem.HttpMethod.POST);
        assertInlineFields(creation,
                "data[ORDER].id",
                "data[ORDER].orderNumber",
                "data[BONUS].id",
                "data[BONUS].amount",
                "resultType");

        String messages = description(openApi, "/api/config/error-messages", PathItem.HttpMethod.GET);
        assertInlineFields(messages, "data", "data.{key}");

        String upload = description(openApi, "/api/config/upload", PathItem.HttpMethod.POST);
        assertInlineFields(upload, "fileName", "url");

        String login = description(openApi, "/api/user/login", PathItem.HttpMethod.POST);
        assertInlineFields(login,
                "data.token",
                "data.user.id",
                "data.user.memberLevel.id",
                "data.user.memberLevel.description");
    }

    @Test
    void usesRealControllerMappingsAndContextSpecificFieldSemantics() {
        OpenAPI openApi = controllerOpenApi();

        Set<String> operationIds = new LinkedHashSet<>();
        openApi.getPaths().values().forEach(pathItem -> pathItem.readOperations().forEach(
                operation -> operationIds.add(operation.getOperationId())));
        assertEquals(CURRENT_H5_OPERATIONS, operationIds);
        assertEquals(CURRENT_H5_OPERATIONS.size(), openApi.getPaths().values().stream()
                .mapToInt(pathItem -> pathItem.readOperations().size()).sum());
        assertTrue(openApi.getPaths().keySet().stream().noneMatch(path -> path.contains("/api/test/")));
        assertNotNull(openApi.getPaths().get("/api/order").getGet());
        assertNotNull(openApi.getPaths().get("/api/order").getPost());
        assertNotNull(openApi.getPaths().get("/api/order/{id}").getGet());
        assertNotNull(openApi.getPaths().get("/api/order/{id}/submit").getPost());
        assertNotNull(openApi.getPaths().get("/api/config/website").getGet());

        Schema<?> order = component(openApi, "OrderResponse");
        assertEquals("订单类型：0 普通订单，1 连单", property(order, "type").getDescription());
        assertEquals("订单状态：0 已完成，1 待提交，2 冻结，3 已取消",
                property(order, "status").getDescription());

        assertEquals("性别：0 未知，1 男，2 女",
                property(component(openApi, "RegisterDto"), "gender").getDescription());
        assertEquals("性别：0 未知，1 男，2 女",
                property(component(openApi, "UserProfileResponse"), "gender").getDescription());
        assertEquals("累计已发放彩金金额；无已发放彩金时为 0",
                property(component(openApi, "UserProfileResponse"), "luckyBonus").getDescription());

        Schema<?> withdrawalType = component(openApi, "WithdrawalTypeResponse");
        assertEquals("提现方式类别：0 银行卡，1 数字钱包",
                property(withdrawalType, "type").getDescription());
        Schema<?> withdrawalAccount = component(openApi, "WithdrawalAccountResponse");
        assertEquals("提现账户类别：0 银行卡，1 数字钱包",
                property(withdrawalAccount, "type").getDescription());

        Schema<?> bonus = component(openApi, "BonusResponse");
        assertEquals("彩金动画持续时间，单位秒",
                property(bonus, "animationDuration").getDescription());
        assertEquals("彩金弹窗展示时间，单位秒",
                property(bonus, "displayDuration").getDescription());

        Parameter orderStatus = openApi.getPaths().get("/api/order").getGet().getParameters()
                .stream().filter(parameter -> "status".equals(parameter.getName()))
                .findFirst().orElseThrow();
        assertTrue(orderStatus.getDescription().contains("2 冻结"));
        assertTrue(orderStatus.getDescription().contains("3 已取消"));
        assertTrue(orderStatus.getDescription().contains("不接受 3"));

        io.swagger.v3.oas.models.Operation login =
                openApi.getPaths().get("/api/user/login").getPost();
        assertFalse(login.getResponses().containsKey("401"));
        assertTrue(login.getResponses().keySet().containsAll(Set.of("400", "405", "500")));
        io.swagger.v3.oas.models.Operation goods =
                openApi.getPaths().get("/api/goods/getGoodsList").getGet();
        assertTrue(goods.getResponses().keySet().containsAll(Set.of("400", "401", "405", "500")));

        Schema<?> success = login.getResponses().get("200")
                .getContent().get("application/json").getSchema();
        String msgDescription = property(success, "msg").getDescription();
        assertTrue(msgDescription.contains("固定英文安全提示"));
        assertTrue(msgDescription.contains("同一 code"));
        assertFalse(msgDescription.contains("与 code 对应"));
    }

    @Test
    void doesNotCustomizeManagementPaths() {
        io.swagger.v3.oas.models.Operation operation =
                new io.swagger.v3.oas.models.Operation().operationId("login");
        OpenAPI openApi = new OpenAPI().paths(new Paths()
                .addPathItem("/system/user/list", new PathItem().get(operation)));

        H5OpenApiConfig.customize(openApi);

        assertNull(operation.getDescription());
        assertNull(operation.getResponses());
    }

    private static void inspectSchema(
            OpenAPI openApi,
            Schema<?> schema,
            String path,
            List<String> failures,
            Set<String> visitingRefs) {
        if (schema == null) {
            return;
        }
        if (schema.get$ref() != null) {
            String name = schema.get$ref().substring(schema.get$ref().lastIndexOf('/') + 1);
            if (visitingRefs.add(name)) {
                Schema<?> referenced = openApi.getComponents().getSchemas().get(name);
                if (referenced == null
                        || referenced.getDescription() == null
                        || !HAN.matcher(referenced.getDescription()).find()
                        || "H5 接口数据模型".equals(referenced.getDescription())) {
                    failures.add(path + " -> " + name + " model description");
                }
                inspectSchema(openApi, referenced, path + " -> " + name, failures, visitingRefs);
                visitingRefs.remove(name);
            }
            return;
        }
        if (schema.getProperties() != null) {
            for (Map.Entry<String, Schema> entry
                    : ((Map<String, Schema>) schema.getProperties()).entrySet()) {
                String description = entry.getValue().getDescription();
                if (description == null
                        || !HAN.matcher(description).find()
                        || description.startsWith("H5 接口字段：")) {
                    failures.add(path + " -> " + entry.getKey() + " field description");
                }
                if ("msg".equals(entry.getKey())) {
                    Object example = entry.getValue().getExample();
                    if (example == null || HAN.matcher(String.valueOf(example)).find()) {
                        failures.add(path + " -> msg example must be non-Chinese");
                    }
                }
                inspectSchema(openApi, entry.getValue(),
                        path + " -> " + entry.getKey(), failures, visitingRefs);
            }
        }
        inspectSchema(openApi, schema.getItems(), path + "[]", failures, visitingRefs);
        inspectSchemas(openApi, schema.getAllOf(), path + " allOf", failures, visitingRefs);
        inspectSchemas(openApi, schema.getOneOf(), path + " oneOf", failures, visitingRefs);
        inspectSchemas(openApi, schema.getAnyOf(), path + " anyOf", failures, visitingRefs);
    }

    private static void inspectSchemas(
            OpenAPI openApi,
            List<Schema> schemas,
            String path,
            List<String> failures,
            Set<String> visitingRefs) {
        if (schemas != null) {
            for (Schema schema : schemas) {
                inspectSchema(openApi, schema, path, failures, visitingRefs);
            }
        }
    }

    private static Schema<?> successSchema(io.swagger.v3.oas.models.Operation operation) {
        String status = operation.getResponses().containsKey("200") ? "200" : "201";
        return operation.getResponses().get(status)
                .getContent().get("application/json").getSchema();
    }

    @SuppressWarnings("unchecked")
    private static void collectInlineFieldPaths(
            OpenAPI openApi,
            Schema<?> schema,
            String path,
            Set<String> paths,
            Set<String> visitingRefs,
            int depth) {
        if (schema == null || depth > 12) {
            return;
        }
        if (schema.get$ref() != null) {
            String name = refName(schema);
            if (visitingRefs.add(name)) {
                collectInlineFieldPaths(
                        openApi,
                        openApi.getComponents().getSchemas().get(name),
                        path,
                        paths,
                        visitingRefs,
                        depth + 1);
                visitingRefs.remove(name);
            }
            return;
        }
        if (schema.getProperties() != null) {
            ((Map<String, Schema<?>>) (Map<?, ?>) schema.getProperties()).forEach((name, property) -> {
                String fieldPath = path.isBlank() ? name : path + "." + name;
                paths.add(fieldPath);
                collectInlineFieldPaths(
                        openApi, property, fieldPath, paths, visitingRefs, depth + 1);
            });
        }
        if (schema.getItems() != null) {
            collectInlineFieldPaths(
                    openApi, schema.getItems(), path + "[]", paths, visitingRefs, depth + 1);
        }
        if (schema.getAdditionalProperties() instanceof Schema<?> additional) {
            String fieldPath = path.isBlank() ? "{key}" : path + ".{key}";
            paths.add(fieldPath);
            collectInlineFieldPaths(
                    openApi, additional, fieldPath, paths, visitingRefs, depth + 1);
        }
        collectInlineComposedPaths(
                openApi, schema.getAllOf(), path, paths, visitingRefs, depth, false);
        collectInlineComposedPaths(
                openApi, schema.getOneOf(), path, paths, visitingRefs, depth, true);
        collectInlineComposedPaths(
                openApi, schema.getAnyOf(), path, paths, visitingRefs, depth, true);
    }

    private static void collectInlineComposedPaths(
            OpenAPI openApi,
            List<Schema> schemas,
            String path,
            Set<String> paths,
            Set<String> visitingRefs,
            int depth,
            boolean labelBranch) {
        if (schemas == null) {
            return;
        }
        for (Schema<?> child : schemas) {
            String childPath = path;
            if (labelBranch) {
                String schemaName = refName(child);
                String label = switch (schemaName) {
                    case "OrderResponse" -> "ORDER";
                    case "BonusResponse" -> "BONUS";
                    default -> schemaName.isBlank() ? "分支" : schemaName;
                };
                childPath = path.isBlank() ? "[" + label + "]" : path + "[" + label + "]";
            }
            collectInlineFieldPaths(
                    openApi, child, childPath, paths, visitingRefs, depth + 1);
        }
    }

    private static String refName(Schema<?> schema) {
        if (schema == null || schema.get$ref() == null) {
            return "";
        }
        return schema.get$ref().substring(schema.get$ref().lastIndexOf('/') + 1);
    }

    private static String description(
            OpenAPI openApi,
            String path,
            PathItem.HttpMethod method) {
        io.swagger.v3.oas.models.Operation operation =
                openApi.getPaths().get(path).readOperationsMap().get(method);
        assertNotNull(operation, () -> "Missing operation " + method + " " + path);
        assertNotNull(operation.getDescription());
        return operation.getDescription();
    }

    private static void assertInlineFields(String description, String... fieldPaths) {
        for (String fieldPath : fieldPaths) {
            assertTrue(description.contains("- `" + fieldPath + "` (`"),
                    () -> "Missing inline response field " + fieldPath + " in:\n" + description);
        }
    }

    private static OpenAPI controllerOpenApi() {
        OpenAPI openApi = new OpenAPI()
                .components(new Components())
                .paths(new Paths());
        ControllerMappingIntrospector mappings = new ControllerMappingIntrospector();

        for (Class<?> controller : H5_CONTROLLERS) {
            for (Method method : controller.getDeclaredMethods()) {
                Operation annotation = method.getAnnotation(Operation.class);
                if (annotation == null) {
                    continue;
                }
                assertTrue(HAN.matcher(annotation.summary()).find(),
                        () -> controller.getSimpleName() + "." + method.getName()
                                + " must have a Chinese summary");
                RequestMappingInfo mapping = mappings.mappingFor(method, controller);
                assertNotNull(mapping, () -> "Missing Spring MVC mapping for "
                        + controller.getSimpleName() + "." + method.getName());
                assertEquals(1, mapping.getPatternValues().size());
                assertEquals(1, mapping.getMethodsCondition().getMethods().size());

                String path = mapping.getPatternValues().iterator().next();
                RequestMethod requestMethod =
                        mapping.getMethodsCondition().getMethods().iterator().next();
                io.swagger.v3.oas.models.Operation operation =
                        new io.swagger.v3.oas.models.Operation()
                                .operationId(method.getName())
                                .summary(annotation.summary());
                describeRealRequest(openApi, operation, method);
                PathItem item = openApi.getPaths().get(path);
                if (item == null) {
                    item = new PathItem();
                    openApi.getPaths().addPathItem(path, item);
                }
                item.operation(PathItem.HttpMethod.valueOf(requestMethod.name()), operation);
            }
        }

        H5OpenApiConfig.customize(openApi);
        return openApi;
    }

    private static void describeRealRequest(
            OpenAPI openApi,
            io.swagger.v3.oas.models.Operation operation,
            Method method) {
        for (java.lang.reflect.Parameter javaParameter : method.getParameters()) {
            org.springframework.web.bind.annotation.RequestBody body =
                    javaParameter.getAnnotation(org.springframework.web.bind.annotation.RequestBody.class);
            if (body != null) {
                Schema<?> schema = resolveSchema(openApi, javaParameter.getParameterizedType());
                operation.setRequestBody(jsonRequestBody(schema, body.required()));
                continue;
            }
            RequestPart part = javaParameter.getAnnotation(RequestPart.class);
            if (part != null) {
                String partName = annotationName(part.name(), part.value(), javaParameter.getName());
                ObjectSchema multipart = new ObjectSchema();
                multipart.setDescription("文件上传请求");
                Schema<?> partSchema = MultipartFile.class.isAssignableFrom(javaParameter.getType())
                        ? new StringSchema().format("binary").description("待上传的图片文件")
                        : resolveSchema(openApi, javaParameter.getParameterizedType());
                multipart.addProperty(partName, partSchema);
                RequestBody requestBody = new RequestBody()
                        .required(part.required())
                        .description("multipart/form-data 文件上传请求")
                        .content(new io.swagger.v3.oas.models.media.Content()
                                .addMediaType("multipart/form-data",
                                        new io.swagger.v3.oas.models.media.MediaType()
                                                .schema(multipart)));
                operation.setRequestBody(requestBody);
                continue;
            }

            Parameter parameter = requestParameter(javaParameter);
            if (parameter != null) {
                operation.addParametersItem(parameter);
            }
        }
    }

    private static Parameter requestParameter(java.lang.reflect.Parameter javaParameter) {
        PathVariable pathVariable = javaParameter.getAnnotation(PathVariable.class);
        if (pathVariable != null) {
            return parameter(
                    annotationName(pathVariable.name(), pathVariable.value(), javaParameter.getName()),
                    "path", true, javaParameter.getType());
        }
        RequestHeader header = javaParameter.getAnnotation(RequestHeader.class);
        if (header != null) {
            return parameter(
                    annotationName(header.name(), header.value(), javaParameter.getName()),
                    "header", header.required(), javaParameter.getType());
        }
        RequestParam query = javaParameter.getAnnotation(RequestParam.class);
        if (query != null) {
            return parameter(
                    annotationName(query.name(), query.value(), javaParameter.getName()),
                    "query", query.required(), javaParameter.getType());
        }
        RequestAttribute attribute = javaParameter.getAnnotation(RequestAttribute.class);
        if (attribute != null) {
            return parameter(
                    annotationName(attribute.name(), attribute.value(), javaParameter.getName()),
                    "query", attribute.required(), javaParameter.getType());
        }
        return null;
    }

    private static Parameter parameter(String name, String in, boolean required, Class<?> type) {
        return new Parameter()
                .name(name)
                .in(in)
                .required(required)
                .schema(simpleSchema(type));
    }

    private static Schema<?> simpleSchema(Class<?> type) {
        if (type == int.class || type == Integer.class) {
            return new IntegerSchema().format("int32");
        }
        if (type == long.class || type == Long.class) {
            return new IntegerSchema().format("int64");
        }
        if (type == boolean.class || type == Boolean.class) {
            return new Schema<>().type("boolean");
        }
        return new StringSchema();
    }

    private static RequestBody jsonRequestBody(Schema<?> schema, boolean required) {
        return new RequestBody()
                .required(required)
                .description("JSON 请求体")
                .content(new io.swagger.v3.oas.models.media.Content()
                        .addMediaType("application/json",
                                new io.swagger.v3.oas.models.media.MediaType().schema(schema)));
    }

    private static Schema<?> resolveSchema(OpenAPI openApi, java.lang.reflect.Type type) {
        ResolvedSchema resolved = ModelConverters.getInstance()
                .resolveAsResolvedSchema(new AnnotatedType(type).resolveAsRef(true));
        if (resolved.referencedSchemas != null) {
            resolved.referencedSchemas.forEach(openApi.getComponents()::addSchemas);
        }
        return resolved.schema == null ? new ObjectSchema() : resolved.schema;
    }

    private static String annotationName(String name, String value, String fallback) {
        if (name != null && !name.isBlank()) {
            return name;
        }
        return value != null && !value.isBlank() ? value : fallback;
    }

    private static Schema<?> component(OpenAPI openApi, String name) {
        Schema<?> schema = openApi.getComponents().getSchemas().get(name);
        assertNotNull(schema, () -> "Missing component schema " + name);
        return schema;
    }

    @SuppressWarnings("unchecked")
    private static Schema<?> property(Schema<?> schema, String name) {
        Schema<?> property = schema.getProperties() == null
                ? null
                : ((Map<String, Schema<?>>) (Map<?, ?>) schema.getProperties()).get(name);
        assertNotNull(property, () -> "Missing property " + name);
        return property;
    }

    private static final class ControllerMappingIntrospector
            extends RequestMappingHandlerMapping {
        private RequestMappingInfo mappingFor(Method method, Class<?> controller) {
            return super.getMappingForMethod(method, controller);
        }
    }
}
