package com.order.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.api.controller.dto.UserProfileResponse;
import com.order.api.service.*;
import com.order.common.i18n.ITranslationsService;
import com.order.common.i18n.SupportedLocale;
import com.order.common.i18n.Translations;
import com.order.framework.config.ServerConfig;
import com.order.member.domain.*;
import com.order.member.mapper.OrderSiteMessageMapper;
import com.order.member.service.*;
import com.order.system.service.ISysNoticeService;
import com.order.system.service.ISysTimeZoneService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RelativeRichTextApiContractTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String ABSOLUTE = "<section><h2>Certificate</h2><p>Text &amp; layout</p>"
            + "<img src=\"https://old.example.com/dev-api/profile/upload/one.png?size=2&amp;x=3\" width=\"120\">"
            + "<img src=\"https://old.example.com/profile/upload/two.png#full\">"
            + "<img src=\"https://external.example.com/image.png\"></section>";
    private static final String RELATIVE = "<section><h2>Certificate</h2><p>Text &amp; layout</p>"
            + "<img src=\"/profile/upload/one.png?size=2&amp;x=3\" width=\"120\">"
            + "<img src=\"/profile/upload/two.png#full\">"
            + "<img src=\"https://external.example.com/image.png\"></section>";
    private static final List<String> FIELDS = List.of("protocolContent", "aboutContent", "certificateContent",
            "helpContent", "termsContent", "eventContent", "transactionDescription", "orderDescription", "usageDescription");
    private static final List<String> TYPES = List.of("register", "about", "certificate", "help", "terms",
            "event", "transaction", "order", "usage");

    private IOrderConfigService configs;
    private IGoodsMemberLevelService levels;
    private ITranslationsService translations;
    private ConfigQueryService queries;
    private MockMvc configMvc;

    @BeforeEach
    void setUp() {
        configs = mock(IOrderConfigService.class);
        levels = mock(IGoodsMemberLevelService.class);
        translations = mock(ITranslationsService.class);
        queries = new ConfigQueryService(configs, mock(IGoodsCustomerServiceService.class), levels,
                mock(ISysNoticeService.class), mock(ISysTimeZoneService.class), translations, JSON);
        configMvc = MockMvcBuilders.standaloneSetup(new ConfigController(queries,
                new ApiLocaleService(), mock(LocalizedMessageCatalogService.class), mock(ServerConfig.class))).build();
        when(translations.selectTranslationsByIds(anyCollection())).thenReturn(Map.of());
    }

    @Test
    void newEndpointReturnsAllNineCompleteHtmlValuesWithoutChangingTheOldEndpoint() throws Exception {
        java.util.ArrayList<OrderConfig> rows = new java.util.ArrayList<>();
        for (int i = 0; i < FIELDS.size(); i++) {
            OrderConfig config = config((long) i + 1, TYPES.get(i), FIELDS.get(i), ABSOLUTE, RELATIVE);
            rows.add(config);
        }
        when(configs.selectOrderConfigByTypes(anyList())).thenReturn(rows);
        var original = configMvc.perform(get("/api/config/content").param("lang", "es_ES"))
                .andExpect(status().isOk());
        var relative = configMvc.perform(get("/api/config/content-relative").param("lang", "es_ES"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_LANGUAGE, "es-ES"))
                .andExpect(jsonPath("$.data.lang").value("es_ES"));
        for (String field : FIELDS) {
            original.andExpect(jsonPath("$.data." + field).value(ABSOLUTE));
            relative.andExpect(jsonPath("$.data." + field).value(RELATIVE));
        }
    }

    @Test
    void relativeEndpointReadsThePersistedCopyFromTheSameLanguageAndFieldFallback() throws Exception {
        OrderConfig config = config(1L, "about", "aboutContent", "<p>Base</p>", "<p>Base</p>");
        config.setTranslationsId(9L);
        Translations record = new Translations();
        record.setId(9L);
        record.setEsEs("{\"unrelated\":\"No about value\"}");
        record.setEnUs(JSON.writeValueAsString(Map.of("aboutContent", ABSOLUTE)));
        // Distinct quote style verifies the stored companion is read, rather than regenerated.
        String persisted = RELATIVE.replace("src=\"/profile/upload/two.png#full\"", "src='/profile/upload/two.png#full'");
        record.setRelativeContent(JSON.writeValueAsString(Map.of(
                "en_US", JSON.writeValueAsString(Map.of("aboutContent", persisted)),
                "es_ES", record.getEsEs())));
        when(configs.selectOrderConfigByTypes(anyList())).thenReturn(List.of(config));
        when(translations.selectTranslationsByIds(anyCollection())).thenReturn(Map.of(9L, record));

        configMvc.perform(get("/api/config/content-relative").param("lang", "es_ES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.aboutContent").value(persisted));
        configMvc.perform(get("/api/config/content").param("lang", "es_ES"))
                .andExpect(jsonPath("$.data.aboutContent").value(ABSOLUTE));
    }

    @Test
    void historicalRowsWithoutCompanionsStillReturnCompleteRelativeHtml() throws Exception {
        OrderConfig config = config(1L, "certificate", "certificateContent", ABSOLUTE, null);
        when(configs.selectOrderConfigByTypes(anyList())).thenReturn(List.of(config));
        configMvc.perform(get("/api/config/content-relative"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.certificateContent").value(RELATIVE))
                .andExpect(jsonPath("$.data.certificateTip").value(""));
    }

    @Test
    void relativeEndpointPreservesCertificateSuppressionAndLocalizedTip() throws Exception {
        OrderConfig config = config(1L, "certificate", "certificateContent", ABSOLUTE, RELATIVE);
        config.setContent(JSON.writeValueAsString(Map.of("certificateContent", ABSOLUTE,
                "certificateSwitch", "1", "certificateTip", "Please contact support")));
        when(configs.selectOrderConfigByTypes(anyList())).thenReturn(List.of(config));
        configMvc.perform(get("/api/config/content-relative"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.certificateContent").value(""))
                .andExpect(jsonPath("$.data.certificateTip").value("Please contact support"));
    }

    @Test
    void memberLevelApiAddsTheCompleteLocalizedRelativeDescription() throws Exception {
        GoodsMemberLevel level = new GoodsMemberLevel();
        level.setId(2L);
        level.setName("VIP");
        level.setDescription("<p>Base</p>");
        level.setDescriptionRelative("<p>Base</p>");
        level.setTranslationsId(9L);
        Translations record = new Translations();
        record.setId(9L);
        record.setEnUs(JSON.writeValueAsString(Map.of("name", "VIP EN", "description", ABSOLUTE)));
        record.setEsEs("{\"name\":\"VIP ES\"}");
        record.setRelativeContent(JSON.writeValueAsString(Map.of(
                "en_US", JSON.writeValueAsString(Map.of("name", "VIP EN", "description", RELATIVE)),
                "es_ES", record.getEsEs())));
        when(levels.selectGoodsMemberLevelList(null)).thenReturn(List.of(level));
        when(translations.selectTranslationsByIds(anyCollection())).thenReturn(Map.of(9L, record));
        configMvc.perform(get("/api/config/member-levels").param("lang", "es_ES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("VIP ES"))
                .andExpect(jsonPath("$.data[0].description").value(ABSOLUTE))
                .andExpect(jsonPath("$.data[0].descriptionRelative").value(RELATIVE));
        level.setTranslations(record);
        queries.localizeMemberLevelInPlace(level, SupportedLocale.ES_ES);
        org.junit.jupiter.api.Assertions.assertEquals(ABSOLUTE, level.getDescription());
        org.junit.jupiter.api.Assertions.assertEquals(RELATIVE, level.getDescriptionRelative());
    }

    @Test
    void messageDetailAddsCompleteRelativeContentWhileListContainsNoBody() throws Exception {
        OrderSiteMessageMapper mapper = mock(OrderSiteMessageMapper.class);
        SiteMessageQueryService service = new SiteMessageQueryService(mapper, translations, JSON);
        OrderSiteMessage message = new OrderSiteMessage();
        message.setId(3L);
        message.setTitle("Base message");
        message.setContent(ABSOLUTE);
        message.setContentRelative(RELATIVE);
        message.setTranslationsId(9L);
        Translations record = new Translations();
        record.setEnUs(JSON.writeValueAsString(Map.of("title", "English message", "content", ABSOLUTE)));
        record.setEsEs("{\"title\":\"Mensaje\"}");
        record.setRelativeContent(JSON.writeValueAsString(Map.of(
                "en_US", JSON.writeValueAsString(Map.of("title", "English message", "content", RELATIVE)),
                "es_ES", record.getEsEs())));
        when(mapper.selectEnabledByIdAndMemberId(3L, 7L)).thenReturn(message);
        when(mapper.selectEnabledByMemberId(7L)).thenReturn(List.of(message));
        when(translations.selectTranslationsById(9L)).thenReturn(record);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new SiteMessageController(service, new ApiLocaleService())).build();
        mvc.perform(get("/api/messages/3").requestAttr("userId", 7L).param("lang", "es_ES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.noticeTitle").value("Mensaje"))
                .andExpect(jsonPath("$.data.noticeContent").value(ABSOLUTE))
                .andExpect(jsonPath("$.data.noticeContentRelative").value(RELATIVE));
        mvc.perform(get("/api/messages").requestAttr("userId", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].noticeContent").doesNotExist())
                .andExpect(jsonPath("$.rows[0].noticeContentRelative").doesNotExist());
    }

    @Test
    void goodsApiKeepsAbsoluteDescriptionAndAddsItsRelativeCompanion() throws Exception {
        IGoodsService service = mock(IGoodsService.class);
        Goods good = new Goods();
        good.setId(4L);
        good.setDescription(ABSOLUTE);
        when(service.selectRandomGoods()).thenReturn(List.of(good));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new GoodsApiController(service)).build();
        mvc.perform(get("/api/goods/getGoodsList"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].description").value(ABSOLUTE))
                .andExpect(jsonPath("$.data[0].descriptionRelative").value(RELATIVE));
    }

    @Test
    void loginAndUserInfoExposeTheRelativeFieldInsideTheExistingMemberLevel() throws Exception {
        UserApiService users = mock(UserApiService.class);
        GoodsMemberLevel level = new GoodsMemberLevel();
        level.setId(2L);
        level.setDescription(ABSOLUTE);
        level.setDescriptionRelative(RELATIVE);
        OrderUser user = new OrderUser();
        user.setId(7L);
        user.setUsername("member");
        user.setMemberLevel(level);
        UserProfileResponse profile = UserProfileResponse.from(user);
        when(users.userInfo(eq(7L), any())).thenReturn(profile);
        when(users.login(any(), any())).thenReturn(new UserLoginResult("test-token", profile));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AuthController(users, mock(ServerConfig.class),
                new ApiLocaleService(), mock(WithdrawalAccountAccessService.class))).build();
        mvc.perform(get("/api/user/getInfo").requestAttr("userId", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberLevel.description").value(ABSOLUTE))
                .andExpect(jsonPath("$.data.memberLevel.descriptionRelative").value(RELATIVE));
        mvc.perform(post("/api/user/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"member\",\"password\":\"test-password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("test-token"))
                .andExpect(jsonPath("$.data.user.memberLevel.description").value(ABSOLUTE))
                .andExpect(jsonPath("$.data.user.memberLevel.descriptionRelative").value(RELATIVE));
    }

    private static OrderConfig config(Long id, String type, String field, String absolute, String relative) throws Exception {
        OrderConfig result = new OrderConfig();
        result.setId(id);
        result.setType(type);
        result.setContent(JSON.writeValueAsString(Map.of(field, absolute)));
        result.setContentRelative(relative == null ? null : JSON.writeValueAsString(Map.of(field, relative)));
        return result;
    }
}
