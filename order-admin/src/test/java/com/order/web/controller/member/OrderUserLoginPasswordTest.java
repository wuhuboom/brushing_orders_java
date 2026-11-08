package com.order.web.controller.member;

import com.order.member.domain.GoodsMemberLevel;
import com.order.member.domain.OrderUser;
import com.order.member.mapper.OrderUserMapper;
import com.order.member.service.MemberDailyStatisticsService;
import com.order.member.service.impl.OrderUserServiceImpl;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrderUserLoginPasswordTest {
    @Mock private OrderUserMapper userMapper;
    @Mock private MemberDailyStatisticsService dailyStatistics;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        OrderUserServiceImpl service = new OrderUserServiceImpl();
        ReflectionTestUtils.setField(service, "orderUserMapper", userMapper);
        ReflectionTestUtils.setField(service, "dailyStatisticsService", dailyStatistics);
        OrderUserController controller = new OrderUserController();
        ReflectionTestUtils.setField(controller, "orderUserService", service);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void resetsOnlyLoginPasswordWhenOtherMemberFieldsAreProvided() throws Exception {
        assertPasswordOnlyUpdate("""
                {
                  "id": 7, "password": "  NewLogin123  ",
                  "taskProgress": 999, "version": 999,
                  "username": "changed", "parentId": 8, "vipId": 9,
                  "tradePassword": "changed", "accountStatus": "1",
                  "balance": 99999, "frozenBalance": 0,
                  "createTime": 0, "updateTime": 0,
                  "params": {"taskProgress": 999}
                }
                """);
    }

    @Test
    void ignoresUnknownFieldsWithoutConvertingTheirTypes() throws Exception {
        assertPasswordOnlyUpdate("""
                {
                  "id": 7, "password": "NewLogin123",
                  "taskProgress": {"invalid": true},
                  "version": "not-a-number", "parentId": [],
                  "balance": "not-an-amount", "tradePassword": {},
                  "updateTime": "not-a-date", "unknownField": [1, 2, 3]
                }
                """);
    }

    @Test
    void rejectsMissingIdBeforeReadingOrWritingMember() throws Exception {
        mockMvc.perform(put("/member/orderuser/editPassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"NewLogin123\",\"taskProgress\":999}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("会员ID不能为空"));

        verifyNoInteractions(userMapper);
    }

    @Test
    void rejectsShortPasswordBeforeReadingOrWritingMember() throws Exception {
        mockMvc.perform(put("/member/orderuser/editPassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":7,\"password\":\" 123 \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("登录密码长度不能少于6位"));

        verifyNoInteractions(userMapper);
    }

    @Test
    void missingMemberDoesNotWritePassword() throws Exception {
        mockMvc.perform(put("/member/orderuser/editPassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":7,\"password\":\"NewLogin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("会员不存在"));

        verify(userMapper, never()).updateOrderUser(any(OrderUser.class));
    }

    @Test
    void concurrentUpdateStillReturnsFailure() throws Exception {
        when(userMapper.selectOrderUserById(7L)).thenReturn(existingMember());
        when(userMapper.updateOrderUser(any(OrderUser.class))).thenReturn(0);

        mockMvc.perform(put("/member/orderuser/editPassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":7,\"password\":\"NewLogin123\",\"version\":999}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        ArgumentCaptor<OrderUser> update = ArgumentCaptor.forClass(OrderUser.class);
        verify(userMapper).updateOrderUser(update.capture());
        assertEquals(4L, update.getValue().getVersion());
    }

    private void assertPasswordOnlyUpdate(String request) throws Exception {
        when(userMapper.selectOrderUserById(7L)).thenReturn(existingMember());
        when(userMapper.updateOrderUser(any(OrderUser.class))).thenReturn(1);

        mockMvc.perform(put("/member/orderuser/editPassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<OrderUser> update = ArgumentCaptor.forClass(OrderUser.class);
        verify(userMapper).updateOrderUser(update.capture());
        OrderUser written = update.getValue();
        assertEquals(7L, written.getId());
        assertEquals(4L, written.getVersion());
        assertNotNull(written.getUpdateTime());
        assertNotEquals("NewLogin123", written.getPassword());
        assertTrue(new BCryptPasswordEncoder().matches("NewLogin123", written.getPassword()));

        Set<String> populated = new HashSet<>();
        for (Class<?> type = OrderUser.class; type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                field.setAccessible(true);
                if (field.get(written) != null) populated.add(field.getName());
            }
        }
        assertEquals(Set.of("id", "password", "version", "updateTime"), populated);
        assertPasswordOnlySql(written);
    }

    private void assertPasswordOnlySql(OrderUser update) throws Exception {
        Configuration configuration = new Configuration();
        configuration.getTypeAliasRegistry().registerAlias("OrderUser", OrderUser.class);
        configuration.getTypeAliasRegistry().registerAlias("GoodsMemberLevel", GoodsMemberLevel.class);
        String resource = "mapper/OrderUserMapper.xml";
        try (InputStream input = getClass().getResourceAsStream("/" + resource)) {
            assertNotNull(input);
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        String sql = configuration
                .getMappedStatement(OrderUserMapper.class.getName() + ".updateOrderUser")
                .getBoundSql(update).getSql().replaceAll("\\s+", " ").trim();
        String assignments = sql.substring(sql.indexOf("SET ") + 4, sql.indexOf(" where "));
        Set<String> columns = Arrays.stream(assignments.split(","))
                .map(assignment -> assignment.split("=", 2)[0].trim())
                .collect(Collectors.toSet());
        assertEquals(Set.of("password", "update_time", "version"), columns);
        assertTrue(sql.endsWith("where id = ? AND version = ?"));
    }

    private static OrderUser existingMember() {
        OrderUser member = new OrderUser();
        member.setId(7L);
        member.setVersion(4L);
        member.setTaskProgress(12L);
        member.setBalance(new BigDecimal("100.00"));
        member.setUsername("original-member");
        return member;
    }
}
