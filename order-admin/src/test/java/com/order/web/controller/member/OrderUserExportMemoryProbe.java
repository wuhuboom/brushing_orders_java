package com.order.web.controller.member;

import com.order.common.core.redis.RedisCache;
import com.order.member.domain.GoodsMemberLevel;
import com.order.member.domain.OrderUser;
import com.order.member.service.IOrderUserService;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipFile;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/** Standalone low-heap probe: run with -Xmx128m; writes and verifies XLSX on disk. */
public final class OrderUserExportMemoryProbe {
    public static void main(String[] args) throws Exception {
        int total = Integer.parseInt(args[0]);
        Path output = Path.of(args[1]);
        AtomicInteger batches = new AtomicInteger();
        AtomicInteger largestBatch = new AtomicInteger();
        AtomicLong observedHeap = new AtomicLong();
        GoodsMemberLevel level = new GoodsMemberLevel();
        level.setName("VIP 3");
        level.setOrderCountPerDay(40L);
        level.setTaskCountPerDay(3L);
        level.setWithdrawCountPerDay(5L);
        level.setWithdrawLimitPerDay(new BigDecimal("100000.00"));
        IOrderUserService service = (IOrderUserService) Proxy.newProxyInstance(
                IOrderUserService.class.getClassLoader(), new Class<?>[]{IOrderUserService.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("selectOrderUserExportMaxId")) return (long) total;
                    if (!method.getName().equals("selectOrderUserExportBatch")) {
                        throw new AssertionError("Unexpected service call: " + method.getName());
                    }
                    long afterId = (Long) arguments[1];
                    long maxId = (Long) arguments[2];
                    int limit = (Integer) arguments[3];
                    if (limit > 500 || maxId != total) throw new AssertionError("Unbounded query");
                    List<OrderUser> members = new ArrayList<>(limit);
                    for (long id = afterId + 1; id <= Math.min(maxId, afterId + limit); id++) {
                        members.add(member(id, level));
                    }
                    batches.incrementAndGet();
                    largestBatch.accumulateAndGet(members.size(), Math::max);
                    observedHeap.accumulateAndGet(ManagementFactory.getMemoryMXBean()
                            .getHeapMemoryUsage().getUsed(), Math::max);
                    return members;
                });
        RedisCache redis = new RedisCache() {
            @Override
            public Set<String> existingKeys(Collection<String> keys) {
                if (keys.size() > 1000) throw new AssertionError("Unbounded Redis query");
                return Set.of();
            }

            @Override
            public Collection<String> keys(String pattern) {
                throw new AssertionError("Export cannot scan Redis");
            }
        };
        OrderUserController controller = new OrderUserController();
        inject(controller, "orderUserService", service);
        inject(controller, "redisCache", redis);
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(), new Class<?>[]{HttpServletRequest.class},
                (proxy, method, arguments) -> null);
        long started = System.nanoTime();
        try (OutputStream file = Files.newOutputStream(output)) {
            ServletOutputStream stream = new ServletOutputStream() {
                @Override public void write(int value) throws java.io.IOException { file.write(value); }
                @Override public void write(byte[] bytes, int offset, int length) throws java.io.IOException {
                    file.write(bytes, offset, length);
                }
                @Override public boolean isReady() { return true; }
                @Override public void setWriteListener(WriteListener listener) { throw new UnsupportedOperationException(); }
            };
            HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(
                    HttpServletResponse.class.getClassLoader(), new Class<?>[]{HttpServletResponse.class},
                    (proxy, method, arguments) -> {
                        if (method.getName().equals("getOutputStream")) return stream;
                        if (method.getReturnType() == boolean.class) return false;
                        if (method.getReturnType() == int.class) return 0;
                        if (method.getName().equals("getCharacterEncoding")) return "UTF-8";
                        return null;
                    });
            controller.export(response, new OrderUser(), request);
        }
        long[] rows = {0};
        int sheets = 0;
        SAXParserFactory parsers = SAXParserFactory.newInstance();
        parsers.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        try (ZipFile zip = new ZipFile(output.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (!entry.getName().matches("xl/worksheets/sheet[0-9]+\\.xml")) continue;
                sheets++;
                try (InputStream xml = zip.getInputStream(entry)) {
                    parsers.newSAXParser().parse(xml, new DefaultHandler() {
                        @Override public void startElement(String uri, String local, String name, Attributes attributes) {
                            if (name.equals("row")) rows[0]++;
                        }
                    });
                }
            }
        }
        if (rows[0] != (long) total + sheets) throw new AssertionError("Dropped or duplicated rows: " + rows[0]);
        if (total > 65536 && sheets < 2) throw new AssertionError("Missing sheet rollover");
        System.out.printf("rows=%d sheets=%d batches=%d maxBatch=%d maxHeapMiB=%d observedHeapMiB=%d fileBytes=%d elapsedMs=%d%n",
                total, sheets, batches.get(), largestBatch.get(), Runtime.getRuntime().maxMemory() / 1024 / 1024,
                observedHeap.get() / 1024 / 1024, Files.size(output), (System.nanoTime() - started) / 1_000_000);
    }

    private static void inject(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static OrderUser member(long id, GoodsMemberLevel level) {
        OrderUser user = new OrderUser();
        user.setId(id);
        user.setUsername("member-" + id);
        user.setPhoneNumber("001380000" + id);
        user.setEmail("member-" + id + "@example.test");
        user.setGender("0");
        user.setBirthday(new Date(0));
        user.setVipId(3L);
        user.setMemberLevel(level);
        user.setParentId(1L);
        user.setParentUsername("parent");
        user.setParentInviteCode("AB1CD");
        user.setInviteCode("CODE-" + id);
        user.setDirectChildrenCount(2);
        user.setCreateTime(new Date(1700000000000L));
        user.setLastLoginTime(new Date(1800000000000L));
        user.setLastLoginIp("127.0.0.1");
        user.setLastLoginAddress("Test address " + id);
        BigDecimal amount = new BigDecimal(id + ".25");
        user.setBalance(amount);
        user.setFrozenBalance(amount);
        user.setBaseSalary(amount);
        user.setTodayCommission(amount);
        user.setTotalCommission(amount);
        user.setTodayParentCommission(amount);
        user.setLuckyBonus(amount);
        user.setPendingBonusAmount(amount);
        user.setTotalRechargeAmount(amount);
        user.setTotalWithdrawalAmount(amount);
        user.setTodayWithdrawalCount(2);
        user.setMaxSingleWithdrawal(amount);
        user.setTaskProgress(4L);
        user.setTodayRest(1);
        user.setTotalRest(10);
        user.setWorkLimit(amount);
        user.setReputationScore(100L);
        user.setSignDays(3);
        user.setTodaySignCount(1);
        user.setTotalSignDays(5);
        user.setIsEnabled("0");
        user.setAllowInvite("0");
        user.setIsFrozen("1");
        user.setIsFake("1");
        user.setIsBanned("1");
        user.setProductMatching("0");
        user.setAccountStatus("0");
        user.setTransactionStatus("0");
        user.setWithdrawalStatus("0");
        user.setAssistWithdrawalStatus("0");
        user.setDepositBlockWithdrawal("1");
        user.setIsWithdrawalNotification("1");
        user.setWeb3AuthEnabled("1");
        user.setIsInvalid("1");
        user.setIsActivity("0");
        user.setVerifyIdentityBeforeTask("1");
        user.setUserContractEnabled("0");
        user.setUserContractSigned("0");
        user.setFormalContractEnabled("0");
        user.setFormalContractSigned("0");
        user.setWithdrawalBlockRemark("Test withdrawal remark " + id);
        user.setRemarks("Test member export memory usage without retaining prior batches " + id);
        return user;
    }
}
