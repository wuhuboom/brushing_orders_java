package com.order.member.service;

import com.alibaba.druid.pool.DruidDataSource;
import com.order.member.domain.dto.TransactionDto;
import com.order.member.service.impl.SequenceAllocator;
import org.junit.jupiter.api.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Destructive tests accept only an explicitly selected, loopback, dedicated test database. */
class AdminTransactionConcurrencyMySqlTest {
    private DruidDataSource business;
    private DruidDataSource sequences;
    private JdbcTemplate jdbc;
    private AdminTransactionRequestService service;
    private ITransactionService money;
    private SequenceAllocator allocator;
    private ExecutorService workers;

    @BeforeEach void setup() throws Exception {
        String url = System.getProperty("admin.transaction.jdbcUrl");
        assumeTrue(url != null, "Set admin.transaction.jdbcUrl to an isolated MySQL test database");
        URI endpoint = URI.create(url.substring("jdbc:".length()));
        assertEquals("127.0.0.1", endpoint.getHost());
        assertEquals("/codex_admin_transaction_test", endpoint.getPath());
        assertTrue(endpoint.getPort() > 0);
        assertEquals("127.0.0.1:" + endpoint.getPort(), endpoint.getRawAuthority());
        assertNull(endpoint.getRawQuery()); assertNull(endpoint.getRawFragment());
        business = pool(url, 12); sequences = pool(url, 2);
        jdbc = new JdbcTemplate(business);
        assertEquals("codex_admin_transaction_test", jdbc.queryForObject("SELECT DATABASE()", String.class));
        jdbc.execute("DROP TABLE IF EXISTS admin_transaction_request");
        jdbc.execute("DROP TABLE IF EXISTS transaction_test_balance");
        jdbc.execute("DROP TABLE IF EXISTS order_sequence_manager");
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/mysql/V2026_10_27_00__admin_transaction_requests.sql")).execute(business);
        jdbc.execute("CREATE TABLE transaction_test_balance (id BIGINT PRIMARY KEY, balance DECIMAL(18,2)) ENGINE=InnoDB");
        jdbc.execute("INSERT INTO transaction_test_balance VALUES (213,0),(122,0)");
        jdbc.execute("""
            CREATE TABLE order_sequence_manager (
              id BIGINT AUTO_INCREMENT PRIMARY KEY, seq_type VARCHAR(64), seq_date CHAR(8), seq_time CHAR(4),
              current_seq BIGINT, version BIGINT, create_time DATETIME(3), update_time DATETIME(3),
              UNIQUE KEY uk_type_date_time (seq_type,seq_date,seq_time)) ENGINE=InnoDB
            """);
        money = mock(ITransactionService.class);
        allocator = new SequenceAllocator(sequences);
        service = new AdminTransactionRequestService(business, new DataSourceTransactionManager(business), money);
        workers = Executors.newFixedThreadPool(10);
        when(money.processTransaction(any())).thenAnswer(invocation -> {
            TransactionDto dto = invocation.getArgument(0);
            jdbc.update("UPDATE transaction_test_balance SET balance=balance+? WHERE id=?", dto.getAmount(),dto.getUserId());
            allocator.allocate("TRADE_NO","20261027","0117");
            return true;
        });
    }
    private DruidDataSource pool(String url, int size) {
        DruidDataSource source=new DruidDataSource();
        source.setUrl(url); source.setUsername("root"); source.setPassword("");
        source.setMaxActive(size); source.setMaxWait(3000); source.setSocketTimeout(5000);
        source.setConnectionInitSqls(List.of("SET SESSION innodb_lock_wait_timeout=3"));
        return source;
    }
    @AfterEach void cleanup() {
        if (workers != null) workers.shutdownNow();
        if (business != null) business.close();
        if (sequences != null) sequences.close();
    }
    private TransactionDto intent(String key, long user, String amount) {
        TransactionDto dto=new TransactionDto();dto.setRequestId(key);dto.setUserId(user);
        dto.setOperationType(0);dto.setAmount(new BigDecimal(amount));dto.setTransactionType("ck");return dto;
    }
    private BigDecimal balance() { return jdbc.queryForObject("SELECT balance FROM transaction_test_balance WHERE id=213",BigDecimal.class); }
    @Test void concurrentIdenticalRequestsCreditExactlyOnceAndSurviveServiceRestart() throws Exception {
        var dto=intent("same-request-0001",213,"405");
        CountDownLatch start=new CountDownLatch(1);
        List<Future<AdminTransactionRequestService.Result>> results=new ArrayList<>();
        for(int i=0;i<8;i++) results.add(workers.submit(()->{start.await();return service.submit(1L,dto);}));
        start.countDown();
        for(var result:results) assertEquals("SUCCEEDED",result.get(10,TimeUnit.SECONDS).state());
        assertEquals(new BigDecimal("405.00"),balance()); verify(money,times(1)).processTransaction(any());
        var restarted=new AdminTransactionRequestService(business,new DataSourceTransactionManager(business),money);
        assertEquals("SUCCEEDED",restarted.submit(1L,dto).state());
        verify(money,times(1)).processTransaction(any());
        assertThrows(IllegalArgumentException.class,()->restarted.submit(1L,intent("same-request-0001",213,"406")));
        assertEquals("UNKNOWN",restarted.status(2L,dto.getRequestId()).state());
    }
    @Test void failedMoneyWriteRollsBackAndFailureIsDurable() {
        doAnswer(invocation->{
            jdbc.update("UPDATE transaction_test_balance SET balance=balance+405 WHERE id=213");
            throw new IllegalStateException("simulated downstream failure");
        }).when(money).processTransaction(any());
        var dto=intent("failed-request-01",213,"405");
        assertEquals("FAILED",service.submit(1L,dto).state());
        assertEquals(new BigDecimal("0.00"),balance());
        assertEquals("FAILED",service.submit(1L,dto).state());
        verify(money,times(1)).processTransaction(any());
    }
    @Test void inFlightRequestIsUnknownThenResolvesWithoutAnotherCredit() throws Exception {
        CountDownLatch entered=new CountDownLatch(1), release=new CountDownLatch(1);
        doAnswer(invocation->{
            jdbc.update("UPDATE transaction_test_balance SET balance=balance+405 WHERE id=213");
            entered.countDown(); assertTrue(release.await(5,TimeUnit.SECONDS)); return true;
        }).when(money).processTransaction(any());
        var dto=intent("pending-request-1",213,"405");
        var first=workers.submit(()->service.submit(1L,dto));
        assertTrue(entered.await(3,TimeUnit.SECONDS));
        assertEquals("UNKNOWN",service.status(1L,dto.getRequestId()).state());
        var retry=workers.submit(()->service.submit(1L,dto));
        release.countDown();
        assertEquals("SUCCEEDED",first.get(5,TimeUnit.SECONDS).state());
        assertEquals("SUCCEEDED",retry.get(5,TimeUnit.SECONDS).state());
        assertEquals(new BigDecimal("405.00"),balance());verify(money,times(1)).processTransaction(any());
    }
    @Test void independentSequenceDoesNotWaitForBusinessPoolAndSurvivesRollback() throws Exception {
        business.setMaxActive(1);
        var outer=new TransactionTemplate(new DataSourceTransactionManager(business));
        outer.execute(status->{
            jdbc.update("UPDATE transaction_test_balance SET balance=10 WHERE id=213");
            assertEquals(1,allocator.allocate("TRADE_NO","20261027","0117"));
            status.setRollbackOnly();return null;
        });
        assertEquals(new BigDecimal("0.00"),balance());
        assertEquals(2,allocator.allocate("TRADE_NO","20261027","0117"));
        List<Future<Long>> tasks=new ArrayList<>();
        for(int i=0;i<30;i++)tasks.add(workers.submit(()->allocator.allocate("TRADE_NO","20261027","0118")));
        Set<Long> unique=new HashSet<>();for(var task:tasks)unique.add(task.get(10,TimeUnit.SECONDS));
        assertEquals(30,unique.size());assertEquals(30,Collections.max(unique));
    }
    @Test void changedPayloadRacingWithOriginalCannotCreditAgain() throws Exception {
        var one=intent("conflicting-key-1",213,"405");
        var two=intent("conflicting-key-1",213,"810");
        var first=workers.submit(()->service.submit(1L,one));
        var second=workers.submit(()->service.submit(1L,two));
        int succeeded=0, rejected=0;
        for(var task:List.of(first,second)) {
            try { assertEquals("SUCCEEDED",task.get(10,TimeUnit.SECONDS).state());succeeded++; }
            catch(ExecutionException e) {assertInstanceOf(IllegalArgumentException.class,e.getCause());rejected++;}
        }
        assertEquals(1,succeeded);assertEquals(1,rejected);verify(money,times(1)).processTransaction(any());
    }
    @Test void businessUniqueConstraintFailureIsNotMistakenForReplayedRequest() {
        doThrow(new org.springframework.dao.DuplicateKeyException("unrelated flow key")).when(money).processTransaction(any());
        assertEquals("FAILED",service.submit(1L,intent("business-dup-key1",213,"405")).state());
        assertEquals(new BigDecimal("0.00"),balance());
    }
    @Test void invalidMoneyParametersHaveDurableFailedResultsWithoutTouchingMoney() {
        List<TransactionDto> invalid = new ArrayList<>();
        var missingAmount = intent("invalid-amount-null",213,"405");
        missingAmount.setAmount(null); invalid.add(missingAmount);
        invalid.add(intent("invalid-amount-zero",213,"0"));
        invalid.add(intent("invalid-amount-minus",213,"-1"));
        var missingOperation = intent("invalid-op-missing",213,"405");
        missingOperation.setOperationType(null); invalid.add(missingOperation);
        var invalidOperation = intent("invalid-op-unknown",213,"405");
        invalidOperation.setOperationType(2); invalid.add(invalidOperation);
        var negativeGift = intent("invalid-gift-minus",213,"405");
        negativeGift.setGiftAmount(new BigDecimal("-1")); invalid.add(negativeGift);
        for (var dto : invalid) {
            assertEquals("FAILED", service.submit(1L,dto).state());
            assertEquals("FAILED", service.status(1L,dto.getRequestId()).state());
            assertEquals("FAILED", service.submit(1L,dto).state());
        }
        assertEquals(new BigDecimal("0.00"),balance());
        verifyNoInteractions(money);
        // Null and numeric zero are different payloads even when both fail validation.
        var changed = intent("invalid-amount-null",213,"0");
        assertThrows(IllegalArgumentException.class,()->service.submit(1L,changed));
    }
    @Test void invalidChangedPayloadCannotOverwriteCommittedSuccess() {
        String key="success-invalid-change";
        assertEquals("SUCCEEDED",service.submit(1L,intent(key,213,"405")).state());
        var invalid = intent(key,213,"405");
        invalid.setAmount(null);
        assertThrows(IllegalArgumentException.class,()->service.submit(1L,invalid));
        invalid.setAmount(BigDecimal.ZERO);
        assertThrows(IllegalArgumentException.class,()->service.submit(1L,invalid));
        invalid.setAmount(new BigDecimal("405")); invalid.setOperationType(2);
        assertThrows(IllegalArgumentException.class,()->service.submit(1L,invalid));
        invalid.setOperationType(0); invalid.setGiftAmount(new BigDecimal("-1"));
        assertThrows(IllegalArgumentException.class,()->service.submit(1L,invalid));
        assertEquals("SUCCEEDED",service.status(1L,key).state());
        assertEquals(new BigDecimal("405.00"),balance());
        verify(money,times(1)).processTransaction(any());
    }

    @Test void anotherUserCanAllocateWhileFirstBusinessTransactionRemainsOpen() throws Exception {
        var allocated=new CountDownLatch(1);
        var release=new CountDownLatch(1);
        var outer=new TransactionTemplate(new DataSourceTransactionManager(business));
        var first=workers.submit(()->outer.execute(status->{
            jdbc.update("UPDATE transaction_test_balance SET balance=1 WHERE id=213");
            long number=allocator.allocate("TRADE_NO","20261027","0117");
            allocated.countDown();
            try { assertTrue(release.await(5,TimeUnit.SECONDS)); }
            catch(InterruptedException e) { throw new IllegalStateException(e); }
            return number;
        }));
        try {
            assertTrue(allocated.await(3,TimeUnit.SECONDS));
            var second=workers.submit(()->outer.execute(status->{
                jdbc.update("UPDATE transaction_test_balance SET balance=2 WHERE id=122");
                return allocator.allocate("TRADE_NO","20261027","0117");
            }));
            assertEquals(2L,second.get(2,TimeUnit.SECONDS));
        } finally { release.countDown(); }
        assertEquals(1L,first.get(3,TimeUnit.SECONDS));
    }

}
