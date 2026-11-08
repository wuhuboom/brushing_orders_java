package com.order.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.order.api.controller.dto.AccountApiDtos;
import com.order.api.controller.dto.ConfigApiDtos;
import com.order.api.controller.dto.OrderApiDtos;
import com.order.api.controller.dto.UserProfileResponse;
import com.order.api.service.ApiLocaleService;
import com.order.api.service.SiteMessageQueryService;
import com.order.api.service.UserApiService;
import com.order.api.service.WithdrawalAccountAccessService;
import com.order.api.service.WithdrawalAccountApplicationService;
import com.order.api.service.WithdrawalApplicationService;
import com.order.common.core.domain.AjaxResult;
import com.order.common.core.domain.entity.SysUser;
import com.order.common.core.domain.model.LoginUser;
import com.order.common.core.page.TableDataInfo;
import com.order.common.i18n.SupportedLocale;
import com.order.framework.config.ServerConfig;
import com.order.member.domain.Goods;
import com.order.member.domain.OrderBonusTable;
import com.order.member.domain.OrderInfo;
import com.order.member.domain.OrderUser;
import com.order.system.domain.SysTimeZone;
import com.order.system.service.ISysTimeZoneService;
import com.order.system.service.ISystemAlignmentService;
import com.order.web.controller.advice.PhoneMaskingResponseBodyAdvice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PublicApiTimestampContractTest {
    private static final long TIMESTAMP = Instant.parse("2026-07-11T12:00:00.456Z").toEpochMilli();

    @Mock
    private WithdrawalAccountApplicationService accountService;
    @Mock
    private WithdrawalApplicationService withdrawalService;
    @Mock
    private WithdrawalAccountAccessService accessService;
    @Mock
    private SiteMessageQueryService messageService;

    @Mock
    private ISysTimeZoneService timeZoneService;

    private ObjectMapper objectMapper;
    private ApiDateTimeResponseAdvice dateTimeAdvice;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Use Boot's global date setting, independent of standalone MVC defaults.
        objectMapper = Jackson2ObjectMapperBuilder.json()
                .timeZone(TimeZone.getDefault())
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        dateTimeAdvice = new ApiDateTimeResponseAdvice(objectMapper, timeZoneService);
        lenient().when(timeZoneService.getActive()).thenReturn(zone("America/New_York"));
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AccountController(accountService, withdrawalService, accessService),
                        new SiteMessageController(messageService, new ApiLocaleService()))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setControllerAdvice(new ApiResponseLocalizationAdvice(), dateTimeAdvice)
                .build();
    }

    static Stream<Date> dates() {
        return Stream.of(new Date(TIMESTAMP), null);
    }

    @Test
    void mapperDoesNotGloballyChangeManagementDateSerialization() throws Exception {
        assertFalse(objectMapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));
        assertTrue(objectMapper.readTree(objectMapper.writeValueAsString(new Date(TIMESTAMP))).isTextual());
    }

    @ParameterizedTest
    @MethodSource("dates")
    void withdrawalAccountListReturnsFormattedDatesAndTimestamps(Date date) throws Exception {
        when(accountService.list(7L)).thenReturn(List.of(account(date)));

        mockMvc.perform(get("/api/account/withdrawal-accounts").requestAttr("userId", 7L))
                .andExpect(status().isOk())
                .andExpect(dateFields("/data/0/createTime", date))
                .andExpect(dateFields("/data/0/updateTime", updated(date)));
    }

    @ParameterizedTest
    @MethodSource("dates")
    void withdrawalAccountCreationReturnsFormattedDatesAndTimestamps(Date date) throws Exception {
        when(accountService.create(eq(7L), any(AccountApiDtos.WithdrawalAccountRequest.class)))
                .thenReturn(account(date));

        mockMvc.perform(post("/api/account/withdrawal-accounts")
                        .requestAttr("userId", 7L)
                        .param("token", "test-access")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"withdrawalTypeId\":\"3\",\"walletAddress\":\"test-wallet\"}"))
                .andExpect(status().isCreated())
                .andExpect(dateFields("/data/createTime", date))
                .andExpect(dateFields("/data/updateTime", updated(date)));
    }

    @ParameterizedTest
    @MethodSource("dates")
    void withdrawalAccountDetailReturnsFormattedDatesAndTimestamps(Date date) throws Exception {
        when(accountService.getForEdit(7L, 3L)).thenReturn(account(date));

        mockMvc.perform(get("/api/account/withdrawal-accounts/3")
                        .requestAttr("userId", 7L)
                        .param("token", "test-access"))
                .andExpect(status().isOk())
                .andExpect(dateFields("/data/createTime", date))
                .andExpect(dateFields("/data/updateTime", updated(date)));
    }

    @ParameterizedTest
    @MethodSource("dates")
    void withdrawalAccountUpdateReturnsFormattedDatesAndTimestamps(Date date) throws Exception {
        when(accountService.update(eq(7L), eq(3L), any(AccountApiDtos.WithdrawalAccountRequest.class)))
                .thenReturn(account(date));

        mockMvc.perform(put("/api/account/withdrawal-accounts/3")
                        .requestAttr("userId", 7L)
                        .param("token", "test-access")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"withdrawalTypeId\":\"3\",\"walletAddress\":\"test-wallet\"}"))
                .andExpect(status().isOk())
                .andExpect(dateFields("/data/createTime", date))
                .andExpect(dateFields("/data/updateTime", updated(date)));
    }

    @ParameterizedTest
    @MethodSource("dates")
    void withdrawalSubmissionReturnsFormattedDateAndTimestamp(Date date) throws Exception {
        when(withdrawalService.submit(eq(7L), any(AccountApiDtos.WithdrawalRequest.class)))
                .thenReturn(new AccountApiDtos.WithdrawalResponse(
                        12L, "W-TIMESTAMP", BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, "1", date));

        mockMvc.perform(post("/api/account/withdrawals")
                        .requestAttr("userId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requestId":"c0c7a468-c386-48e8-8a8d-64c93d66153a",
                                 "amount":10.00,"tradePassword":"test-password","withdrawalAccountId":3}
                                """))
                .andExpect(status().isCreated())
                .andExpect(dateFields("/data/createdAt", date));
    }

    @ParameterizedTest
    @MethodSource("dates")
    void withdrawalHistoryReturnsFormattedDateAndTimestamp(Date date) throws Exception {
        when(withdrawalService.withdrawalHistory(7L, null, 1, 20))
                .thenReturn(page(new AccountApiDtos.WithdrawalHistoryResponse(
                        12L, "W-TIMESTAMP", BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN,
                        "1", null, "test-account", date)));

        mockMvc.perform(get("/api/account/withdrawals").requestAttr("userId", 7L))
                .andExpect(status().isOk())
                .andExpect(dateFields("/rows/0/createTime", date));
    }

    @ParameterizedTest
    @MethodSource("dates")
    void depositsReturnFormattedDateAndTimestamp(Date date) throws Exception {
        when(withdrawalService.deposits(7L, 1, 20))
                .thenReturn(page(new AccountApiDtos.DepositResponse(
                        12L, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN,
                        "1", "deposit", "D-TIMESTAMP", null, date)));

        mockMvc.perform(get("/api/account/deposits").requestAttr("userId", 7L))
                .andExpect(status().isOk())
                .andExpect(dateFields("/rows/0/createTime", date));
    }

    @ParameterizedTest
    @MethodSource("dates")
    void transactionsReturnFormattedDateAndTimestamp(Date date) throws Exception {
        when(withdrawalService.transactions(7L, 1, 20))
                .thenReturn(page(new AccountApiDtos.TransactionResponse(
                        12L, "T-TIMESTAMP", "deposit", BigDecimal.TEN,
                        BigDecimal.ZERO, BigDecimal.TEN, "deposit", null, date)));

        mockMvc.perform(get("/api/account/transactions").requestAttr("userId", 7L))
                .andExpect(status().isOk())
                .andExpect(dateFields("/rows/0/createdTime", date));
    }

    @ParameterizedTest
    @MethodSource("dates")
    void messageListReturnsFormattedDateAndTimestamp(Date date) throws Exception {
        when(messageService.list(7L, SupportedLocale.EN_US, 1, 20))
                .thenReturn(new ConfigApiDtos.PagedResult<>(
                        List.of(new ConfigApiDtos.NoticeSummaryResponse(12L, "Test message", date)), 1L));

        mockMvc.perform(get("/api/messages").requestAttr("userId", 7L).param("lang", "en-us"))
                .andExpect(status().isOk())
                .andExpect(dateFields("/rows/0/createTime", date));
    }

    @ParameterizedTest
    @MethodSource("dates")
    void messageDetailReturnsFormattedDateAndTimestamp(Date date) throws Exception {
        when(messageService.get(7L, 12L, SupportedLocale.EN_US))
                .thenReturn(new ConfigApiDtos.NoticeDetailResponse(12L, "Test message", "Test body", date));

        mockMvc.perform(get("/api/messages/12").requestAttr("userId", 7L).param("lang", "en-us"))
                .andExpect(status().isOk())
                .andExpect(dateFields("/data/createTime", date));
    }

    @ParameterizedTest
    @MethodSource("dates")
    void orderUserAndGoodsDatesAlsoUseTheApiDateContract(Date date) {
        OrderInfo order = new OrderInfo();
        order.setCreateTime(date);
        order.setExpiryTime(updated(date));
        JsonNode orderJson = apiTree(OrderApiDtos.OrderResponse.from(order));
        assertDateFields(orderJson, "/createTime", date);
        assertDateFields(orderJson, "/expiryTime", updated(date));

        OrderBonusTable bonus = new OrderBonusTable();
        bonus.setExpiryTime(date);
        assertDateFields(apiTree(OrderApiDtos.BonusResponse.from(bonus)), "/expiryTime", date);

        OrderUser user = new OrderUser();
        user.setBirthday(date);
        assertDateFields(apiTree(UserProfileResponse.from(user)), "/birthday", date);

        Goods goods = new Goods();
        goods.setCreateTime(date);
        goods.setUpdateTime(updated(date));
        JsonNode goodsJson = apiTree(goods);
        assertDateFields(goodsJson, "/createTime", date);
        assertDateFields(goodsJson, "/updateTime", updated(date));
    }

    @Test
    void newYorkWinterAndSummerUseTheirOwnDstOffsetsWithOneZoneRead() {
        Date winter = Date.from(Instant.parse("2026-01-11T12:00:00Z"));
        Date summer = new Date(TIMESTAMP);
        JsonNode body = apiTree(List.of(account(winter), account(summer)));

        assertDatePair(body, "/0/createTime", winter, "2026-01-11T07:00:00.000-05:00");
        assertDatePair(body, "/1/createTime", summer, "2026-07-11T08:00:00.456-04:00");
        verify(timeZoneService, times(1)).getActive();
    }

    @Test
    void timezoneChangesApplyOnTheNextRequestWithoutChangingTheTimestamp() throws Exception {
        Date date = new Date(TIMESTAMP);
        when(accountService.list(7L)).thenReturn(List.of(account(date)));
        when(timeZoneService.getActive()).thenReturn(zone("Asia/Manila"), zone("Africa/Lagos"));

        JsonNode manila = objectMapper.readTree(mockMvc.perform(
                        get("/api/account/withdrawal-accounts").requestAttr("userId", 7L))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
        JsonNode lagos = objectMapper.readTree(mockMvc.perform(
                        get("/api/account/withdrawal-accounts").requestAttr("userId", 7L))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());

        assertDatePair(manila, "/data/0/createTime", date, "2026-07-11T20:00:00.456+08:00");
        assertDatePair(manila, "/data/0/updateTime", updated(date), "2026-07-11T20:00:01.456+08:00");
        assertDatePair(lagos, "/data/0/createTime", date, "2026-07-11T13:00:00.456+01:00");
        assertDatePair(lagos, "/data/0/updateTime", updated(date), "2026-07-11T13:00:01.456+01:00");
        verify(timeZoneService, times(2)).getActive();
    }

    @Test
    void isoDatesKeepThreeMillisecondsAndAnExplicitOffset() {
        Date date = Date.from(OffsetDateTime.parse("2026-08-28T16:50:07.023+08:00").toInstant());
        when(timeZoneService.getActive()).thenReturn(zone("Asia/Shanghai"), zone("UTC"));

        assertDatePair(apiTree(account(date)), "/createTime", date, "2026-08-28T16:50:07.023+08:00");
        assertDatePair(apiTree(account(date)), "/createTime", date, "2026-08-28T08:50:07.023+00:00");
        verify(timeZoneService, times(2)).getActive();
    }

    @Test
    void managementResponsesAreReturnedUntouchedWithoutReadingTheTimezone() {
        Goods goods = new Goods();
        goods.setCreateTime(new Date(TIMESTAMP));
        AjaxResult body = AjaxResult.success(goods);
        JsonNode original = objectMapper.valueToTree(body);

        assertSame(body, write(dateTimeAdvice, body, "/system/withdrawal/list"));
        assertEquals(original, objectMapper.valueToTree(body));
        assertTrue(original.at("/data/createTime").isIntegralNumber());
        assertEquals(TIMESTAMP, original.at("/data/createTime").longValue());
        assertTrue(original.at("/data/createTimeTimestamp").isMissingNode());
        verifyNoInteractions(timeZoneService);
    }

    @Test
    void sqlDateSubclassPreservesTheSameInstantWithoutRequiringToInstantSupport() {
        Date date = new java.sql.Date(TIMESTAMP);

        assertDateFields(apiTree(account(date)), "/createTime", date);
        verify(timeZoneService, times(1)).getActive();
    }

    @Test
    void dateFreeResponsesDoNotReadTimezoneOrTreatTimeStringsAndDurationsAsDates() {
        AjaxResult body = AjaxResult.success(Map.of(
                "tradeTimeRange", List.of("09:00", "18:00"),
                "createdTime", "2026-07-11 12:00:00",
                "submitTaskDelayMs", 500));

        assertEquals(objectMapper.valueToTree(body), apiTree(body));
        verifyNoInteractions(timeZoneService);
    }

    @Test
    void nullDatePairsArePreservedWithoutReadingTheTimezone() {
        JsonNode body = apiTree(account(null));

        assertDateFields(body, "/createTime", null);
        assertDateFields(body, "/updateTime", null);
        verifyNoInteractions(timeZoneService);
    }

    @Test
    void dateFormattingAndPhoneMaskingBothApplyInTheProductionAdviceOrder() throws Exception {
        ISystemAlignmentService alignmentService = mock(ISystemAlignmentService.class);
        when(alignmentService.shouldHidePhone(9L)).thenReturn(true);
        PhoneMaskingResponseBodyAdvice phoneAdvice = new PhoneMaskingResponseBodyAdvice();
        ReflectionTestUtils.setField(phoneAdvice, "alignmentService", alignmentService);
        ReflectionTestUtils.setField(phoneAdvice, "objectMapper", objectMapper);
        LoginUser operator = new LoginUser(9L, null, new SysUser(), Set.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(operator, null, List.of()));
        try {
            OrderUser user = new OrderUser();
            user.setPhoneNumber("+12345674455");
            user.setBirthday(new Date(TIMESTAMP));
            UserApiService userService = mock(UserApiService.class);
            when(userService.userInfo(9L, SupportedLocale.EN_US)).thenReturn(UserProfileResponse.from(user));
            MockMvc userMvc = MockMvcBuilders.standaloneSetup(new AuthController(
                            userService, mock(ServerConfig.class), new ApiLocaleService(), accessService))
                    .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                    // Deliberately register in the opposite order; MVC must honor each advice's order.
                    .setControllerAdvice(phoneAdvice, dateTimeAdvice, new ApiResponseLocalizationAdvice())
                    .build();
            JsonNode result = objectMapper.readTree(userMvc.perform(get("/api/user/getInfo")
                            .requestAttr("userId", 9L).param("lang", "en-us"))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
            assertEquals("+12****4455", result.at("/data/phoneNumber").textValue());
            assertDateFields(result, "/data/birthday", new Date(TIMESTAMP));
            assertEquals("Success", result.path("msg").textValue());
            verify(timeZoneService, times(1)).getActive();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private AccountApiDtos.WithdrawalAccountResponse account(Date date) {
        return new AccountApiDtos.WithdrawalAccountResponse(
                3L, "wallet", "3", "Test wallet", true,
                null, null, null, null, null, null, null, "Test wallet", "test-address", null,
                date, updated(date));
    }

    private TableDataInfo page(Object row) {
        TableDataInfo page = new TableDataInfo(List.of(row), 1L);
        page.setCode(200);
        page.setMsg("Success");
        return page;
    }

    private static Date updated(Date date) {
        return date == null ? null : new Date(date.getTime() + 1_000L);
    }

    private ResultMatcher dateFields(String pointer, Date expected) {
        return result -> assertDateFields(
                objectMapper.readTree(result.getResponse().getContentAsByteArray()), pointer, expected);
    }

    private void assertDateFields(JsonNode body, String pointer, Date expected) {
        String formatted = expected == null ? null : expected.getTime() == TIMESTAMP
                ? "2026-07-11T08:00:00.456-04:00" : "2026-07-11T08:00:01.456-04:00";
        assertDatePair(body, pointer, expected, formatted);
    }

    private void assertDatePair(JsonNode body, String pointer, Date expected, String formatted) {
        JsonNode value = body.at(pointer);
        JsonNode timestamp = body.at(pointer + "Timestamp");
        assertFalse(value.isMissingNode(), "Missing date field: " + pointer);
        assertFalse(timestamp.isMissingNode(), "Missing timestamp field: " + pointer);
        if (expected == null) {
            assertTrue(value.isNull(), "Expected JSON null at " + pointer + ", got " + value);
            assertTrue(timestamp.isNull(), "Expected JSON null at " + pointer + "Timestamp, got " + timestamp);
        } else {
            assertTrue(value.isTextual(), "Expected a formatted date string at " + pointer + ", got " + value);
            assertEquals(formatted, value.textValue(), "Wrong active-zone date at " + pointer);
            assertTrue(timestamp.isIntegralNumber(), "Expected JSON integer at " + pointer + "Timestamp");
            assertEquals(expected.getTime(), timestamp.longValue(), "Wrong Unix milliseconds at " + pointer);
            assertEquals(timestamp.longValue(), OffsetDateTime.parse(value.textValue()).toInstant().toEpochMilli(),
                    "ISO date and timestamp must identify the same instant at " + pointer);
        }
    }

    private JsonNode apiTree(Object body) {
        return objectMapper.valueToTree(write(dateTimeAdvice, body, "/api/order"));
    }

    private Object write(ResponseBodyAdvice<Object> advice, Object body, String path) {
        return advice.beforeBodyWrite(body, null, MediaType.APPLICATION_JSON,
                MappingJackson2HttpMessageConverter.class,
                new ServletServerHttpRequest(new MockHttpServletRequest("GET", path)),
                new ServletServerHttpResponse(new MockHttpServletResponse()));
    }

    private static SysTimeZone zone(String name) {
        SysTimeZone zone = new SysTimeZone();
        zone.setTzName(name);
        return zone;
    }
}
