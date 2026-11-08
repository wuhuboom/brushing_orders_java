package com.order.web.controller.member;

import com.github.pagehelper.PageHelper;
import com.order.api.service.AdminMemberCredentialService;
import com.order.common.core.redis.RedisCache;
import com.order.common.exception.ServiceException;
import com.order.member.domain.GoodsMemberLevel;
import com.order.member.domain.OrderUser;
import com.order.member.service.AdminTransactionRequestService;
import com.order.member.service.IGoodsMemberLevelService;
import com.order.member.service.IOrderUserService;
import com.order.member.service.ITransactionService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderUserExportTest {
    @Mock IOrderUserService orderUserService;
    @Mock RedisCache redisCache;
    @InjectMocks OrderUserController controller;

    @AfterEach
    void clearThreadState() {
        PageHelper.clearPage();
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    @Test
    void workbookContainsCoreDataAndPreservesTextIdentifiersAndNumericMoney() throws Exception {
        OrderUser member = member(9007199254740993L, "member-a");
        member.setPhoneNumber("0013800000000");
        member.setEmail("member@example.test");
        member.setBalance(new BigDecimal("3000000000"));
        member.setFrozenBalance(new BigDecimal("12.34"));
        member.setTotalCommission(new BigDecimal("1234.56"));
        member.setTodayCommission(new BigDecimal("12.34"));
        member.setTotalRechargeAmount(new BigDecimal("9876.54"));
        member.setTotalWithdrawalAmount(new BigDecimal("4567.89"));
        member.setLuckyBonus(new BigDecimal("88.88"));
        member.setPendingBonusAmount(new BigDecimal("66.66"));
        member.setTaskProgress(7L);
        member.setPassword("secret-password-hash");
        member.setTradePassword("secret-trade-hash");
        member.setIdentityNumber("private-identity-number");
        member.setUserContractContent("private-contract-body");
        GoodsMemberLevel level = new GoodsMemberLevel();
        level.setName("VIP 3");
        level.setOrderCountPerDay(40L);
        member.setMemberLevel(level);

        OrderUser negative = member(9007199254740994L, "=SUM(1,1)");
        negative.setBalance(new BigDecimal("-42.50"));
        when(orderUserService.selectOrderUserExportMaxId()).thenReturn(negative.getId());
        when(orderUserService.selectOrderUserExportBatch(any(OrderUser.class), eq(0L), eq(negative.getId()), eq(500)))
                .thenReturn(List.of(member, negative));

        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.export(response, new OrderUser(), new MockHttpServletRequest());

        assertTrue(response.getContentType().startsWith("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        try (Workbook workbook = workbook(response)) {
            Sheet sheet = workbook.getSheetAt(0);
            Map<String, Integer> headers = headers(sheet);
            assertEquals(2, sheet.getLastRowNum());
            assertTrue(headers.containsKey("任务进度"));
            assertTrue(headers.containsKey("累计佣金"));
            assertTrue(headers.containsKey("手机号"));
            assertTrue(headers.keySet().stream().noneMatch(name -> name.contains("密码") || name.equals("版本号")));

            Row row = sheet.getRow(1);
            assertText(row, headers, "用户ID", "9007199254740993");
            assertText(row, headers, "手机号", "0013800000000");
            assertMoney(row, headers, "余额", 3000000000d);
            assertMoney(row, headers, "冻结余额", 12.34);
            assertMoney(row, headers, "总余额", 3000000012.34);
            assertMoney(row, headers, "累计佣金", 1234.56);
            assertMoney(row, headers, "累计充值记录金额", 9876.54);
            assertMoney(row, headers, "累计提现申请金额", 4567.89);
            assertMoney(sheet.getRow(2), headers, "余额", -42.50);
            for (Row exported : sheet) {
                for (Cell cell : exported) {
                    assertNotEquals(CellType.FORMULA, cell.getCellType());
                    String value = cell.toString();
                    assertFalse(value.contains("secret-password-hash"));
                    assertFalse(value.contains("secret-trade-hash"));
                    assertFalse(value.contains("private-identity-number"));
                    assertFalse(value.contains("private-contract-body"));
                }
            }
        }
        verify(orderUserService, never()).selectOrderUserById(any());
    }

    @Test
    void exportKeepsBusinessFiltersAndResolvesOnlineUsersWithoutPagination() throws Exception {
        when(redisCache.existingKeys(any())).thenReturn(Set.of("front:user_tokens:7"));
        when(orderUserService.selectOrderUserExportMaxId()).thenReturn(10L);
        OrderUser filter = new OrderUser();
        filter.setKeyword("search-key");
        filter.setUsernameList("id-member,offline-member");
        filter.setBalanceMin(BigDecimal.ZERO);
        filter.getParams().put("beginTime", "2026-11-01");
        filter.getParams().put("endTime", "2026-11-06");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("isOnline", "1");
        request.setParameter("pageNum", "2");
        request.setParameter("pageSize", "1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        when(orderUserService.selectOrderUserExportBatch(filter, 0L, 10L, 500)).thenAnswer(invocation -> {
            assertNull(PageHelper.getLocalPage(), "Export must query all matching rows");
            assertEquals("id-member,offline-member", filter.getUsernameList());
            assertEquals("search-key", filter.getKeyword());
            assertEquals(BigDecimal.ZERO, filter.getBalanceMin());
            assertEquals("2026-11-01", filter.getParams().get("beginTime"));
            assertEquals("2026-11-06", filter.getParams().get("endTime"));
            return List.of(member(7L, "id-member"), member(10L, "offline-member"));
        });
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.export(response, filter, request);
        try (Workbook workbook = workbook(response)) {
            Sheet sheet = workbook.getSheetAt(0);
            assertText(sheet.getRow(1), headers(sheet), "是否在线", "是");
        }
        verify(orderUserService).selectOrderUserExportBatch(filter, 0L, 10L, 500);
        verify(orderUserService, never()).selectOrderUserById(any());
        verify(orderUserService, never()).selectUsernamesByIds(any());
        verify(redisCache, never()).keys(any());
    }

    @Test
    void offlineExportExcludesCurrentAndLegacyOnlineMembers() throws Exception {
        when(redisCache.existingKeys(any())).thenReturn(Set.of("front:user_tokens:7", "user:tokens:legacy-member"));
        when(orderUserService.selectOrderUserExportMaxId()).thenReturn(10L);
        OrderUser filter = new OrderUser();
        filter.setUsernameList("id-member,legacy-member,offline-member");
        when(orderUserService.selectOrderUserExportBatch(filter, 0L, 10L, 500)).thenReturn(List.of(
                member(7L, "id-member"), member(9L, "legacy-member"), member(10L, "offline-member")));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("isOnline", "0");
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.export(response, filter, request);
        assertEquals("id-member,legacy-member,offline-member", filter.getUsernameList());
        try (Workbook workbook = workbook(response)) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals(1, sheet.getLastRowNum());
            assertText(sheet.getRow(1), headers(sheet), "用户ID", "10");
        }
        verify(redisCache, never()).keys(any());
    }

    @Test
    void emptyDatabaseProducesHeadersWithoutFetchingBatches() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("isOnline", "1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.export(response, new OrderUser(), request);
        try (Workbook workbook = workbook(response)) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals(0, sheet.getLastRowNum());
            assertTrue(headers(sheet).containsKey("手机号"));
        }
        verify(orderUserService, never()).selectOrderUserExportBatch(any(), any(), any(), anyInt());
        verifyNoInteractions(redisCache);
    }

    @Test
    void anEmptyFilteredBatchDoesNotStopTheExportBeforeLaterMatchingMembers() throws Exception {
        when(orderUserService.selectOrderUserExportMaxId()).thenReturn(501L);
        when(orderUserService.selectOrderUserExportBatch(any(), eq(0L), eq(501L), eq(500)))
                .thenReturn(LongStream.rangeClosed(1, 500).mapToObj(id -> member(id, "m-" + id)).toList());
        when(orderUserService.selectOrderUserExportBatch(any(), eq(500L), eq(501L), eq(500)))
                .thenReturn(List.of(member(501L, "offline")));
        when(redisCache.existingKeys(any())).thenAnswer(invocation -> {
            List<String> keys = invocation.getArgument(0);
            assertTrue(keys.size() <= 1000);
            return keys.size() == 1000 ? Set.copyOf(keys) : Set.of();
        });
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("isOnline", "0");
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.export(response, new OrderUser(), request);
        try (Workbook workbook = workbook(response)) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals(1, sheet.getLastRowNum());
            assertText(sheet.getRow(1), headers(sheet), "用户ID", "501");
        }
        verify(redisCache, times(2)).existingKeys(any());
    }

    @Test
    void concurrentExportsAreRejectedWithoutQueuingAndPermitIsReleased() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(orderUserService.selectOrderUserExportMaxId()).thenAnswer(invocation -> {
            started.countDown();
            assertTrue(release.await(10, TimeUnit.SECONDS));
            return null;
        });
        var executor = Executors.newSingleThreadExecutor();
        try {
            var first = executor.submit(() -> {
                controller.export(new MockHttpServletResponse(), new OrderUser(), new MockHttpServletRequest());
                return null;
            });
            assertTrue(started.await(10, TimeUnit.SECONDS));
            ServiceException error = assertThrows(ServiceException.class, () -> controller.export(
                    new MockHttpServletResponse(), new OrderUser(), new MockHttpServletRequest()));
            assertEquals("正在导出会员数据，请稍后重试", error.getMessage());
            verify(orderUserService, times(1)).selectOrderUserExportMaxId();
            release.countDown();
            first.get(10, TimeUnit.SECONDS);
            controller.export(new MockHttpServletResponse(), new OrderUser(), new MockHttpServletRequest());
            verify(orderUserService, times(2)).selectOrderUserExportMaxId();
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void failedExportsReleaseThePermitAndDoNotWriteAFile() throws Exception {
        when(orderUserService.selectOrderUserExportMaxId()).thenThrow(new ServiceException("failed"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertThrows(ServiceException.class, () -> controller.export(response, new OrderUser(), new MockHttpServletRequest()));
        assertEquals(0, response.getContentAsByteArray().length);
        doReturn(null).when(orderUserService).selectOrderUserExportMaxId();
        controller.export(response, new OrderUser(), new MockHttpServletRequest());
        assertTrue(response.getContentAsByteArray().length > 0);
    }

    @Test
    void disconnectedDownloadsReleaseThePermit() throws Exception {
        var response = mock(jakarta.servlet.http.HttpServletResponse.class);
        when(response.getOutputStream()).thenThrow(new IOException("disconnected"));
        assertThrows(IOException.class, () -> controller.export(response, new OrderUser(), new MockHttpServletRequest()));
        controller.export(new MockHttpServletResponse(), new OrderUser(), new MockHttpServletRequest());
    }

    @Test
    void exportPermissionIsEnforcedBeforeDataIsRead() throws Exception {
        try (var context = securityContext()) {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("admin", "unused", List.of()));
            OrderUserController secured = context.getBean(OrderUserController.class);
            IOrderUserService service = context.getBean(IOrderUserService.class);
            assertThrows(AccessDeniedException.class, () -> secured.export(
                    new MockHttpServletResponse(), new OrderUser(), new MockHttpServletRequest()));
            verifyNoInteractions(service, context.getBean(RedisCache.class));
            context.getBean(ExportGate.class).allowed = true;
            secured.export(new MockHttpServletResponse(), new OrderUser(), new MockHttpServletRequest());
            verify(service).selectOrderUserExportMaxId();
        }
    }

    private static OrderUser member(Long id, String username) {
        OrderUser user = new OrderUser();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    private static Workbook workbook(MockHttpServletResponse response) throws Exception {
        return WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()));
    }

    private static Map<String, Integer> headers(Sheet sheet) {
        Map<String, Integer> result = new HashMap<>();
        for (Cell cell : sheet.getRow(0)) result.put(cell.getStringCellValue(), cell.getColumnIndex());
        return result;
    }

    private static Cell field(Row row, Map<String, Integer> headers, String name) {
        assertTrue(headers.containsKey(name), "Missing export column: " + name);
        return row.getCell(headers.get(name));
    }

    private static void assertText(Row row, Map<String, Integer> headers, String name, String value) {
        Cell cell = field(row, headers, name);
        assertEquals(CellType.STRING, cell.getCellType());
        assertEquals(value, cell.getStringCellValue());
    }

    private static void assertMoney(Row row, Map<String, Integer> headers, String name, double value) {
        Cell cell = field(row, headers, name);
        assertEquals(CellType.NUMERIC, cell.getCellType());
        assertEquals(value, cell.getNumericCellValue(), 0.00001);
    }

    private static AnnotationConfigApplicationContext securityContext() {
        var context = new AnnotationConfigApplicationContext();
        // Register mocks as existing singletons so Spring does not autowire their internal fields.
        var beans = context.getBeanFactory();
        beans.registerSingleton("userService", mock(IOrderUserService.class));
        beans.registerSingleton("cache", mock(RedisCache.class));
        beans.registerSingleton("levels", mock(IGoodsMemberLevelService.class));
        beans.registerSingleton("transactions", mock(ITransactionService.class));
        beans.registerSingleton("adminTransactions", mock(AdminTransactionRequestService.class));
        beans.registerSingleton("credentials", mock(AdminMemberCredentialService.class));
        context.register(SecurityConfig.class);
        context.refresh();
        return context;
    }

    @Configuration
    @EnableMethodSecurity
    static class SecurityConfig {
        @Bean(name = "ss") ExportGate gate() { return new ExportGate(); }
        @Bean OrderUserController controller() { return new OrderUserController(); }
    }

    public static class ExportGate {
        boolean allowed;
        public boolean hasPermi(String permission) {
            return allowed && "member:orderuser:export".equals(permission);
        }
    }
}
