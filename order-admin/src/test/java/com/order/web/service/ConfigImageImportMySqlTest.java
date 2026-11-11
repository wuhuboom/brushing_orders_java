package com.order.web.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.common.i18n.ITranslationsService;
import com.order.framework.web.service.PermissionService;
import com.order.member.domain.OrderConfig;
import com.order.member.mapper.ConfigImageImportMapper;
import com.order.member.mapper.GoodsMemberLevelMapper;
import com.order.member.mapper.OrderConfigMapper;
import com.order.member.mapper.TranslationsMapper;
import com.order.member.service.IGoodsMemberLevelService;
import com.order.member.service.IOrderConfigService;
import com.order.member.service.impl.GoodsMemberLevelServiceImpl;
import com.order.member.service.impl.OrderConfigServiceImpl;
import com.order.member.service.impl.TranslationsServiceImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.imageio.ImageIO;
import javax.sql.DataSource;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Real transactional import service, business services, MyBatis mappers and InnoDB writes.
 * Only permissions and file storage are controlled; storage still creates real temporary files.
 * Opt in with config.image.import.jdbcUrl pointing to the dedicated loopback test database.
 */
class ConfigImageImportMySqlTest {
    private static final String DATABASE = "codex_config_image_import_test";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final AtomicReference<CyclicBarrier> PLAN_BARRIER = new AtomicReference<>();
    private static final AtomicInteger EVENT_WRITES = new AtomicInteger();
    private static JdbcTemplate jdbc;
    private static AnnotationConfigApplicationContext context;
    private static ConfigImageImportService service;
    private static ConfigImageImportStorage storage;

    @TempDir
    Path files;
    private final List<Path> writtenFiles = new CopyOnWriteArrayList<>();

    @BeforeAll
    static void connectOnlyToTheExplicitIsolatedDatabase() throws Exception {
        String url = System.getProperty("config.image.import.jdbcUrl");
        assumeTrue(url != null, "Set config.image.import.jdbcUrl to an isolated local MySQL test database");
        assertTrue(url.startsWith("jdbc:mysql://"));
        URI endpoint = URI.create(url.substring("jdbc:".length()));
        assertEquals("127.0.0.1", endpoint.getHost());
        assertEquals("/" + DATABASE, endpoint.getPath());
        assertTrue(endpoint.getPort() > 0 && endpoint.getPort() <= 65535);
        assertEquals("127.0.0.1:" + endpoint.getPort(), endpoint.getRawAuthority());
        assertNull(endpoint.getRawQuery());
        assertNull(endpoint.getRawFragment());

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl(url);
        dataSource.setUsername("root");
        dataSource.setPassword("");
        jdbc = new JdbcTemplate(dataSource);
        assertEquals(DATABASE, jdbc.queryForObject("SELECT DATABASE()", String.class));

        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setTypeAliasesPackage("com.order.member.domain");
        factory.setMapperLocations(new ClassPathResource("mapper/GoodsMemberLevelMapper.xml"),
                new ClassPathResource("mapper/OrderConfigMapper.xml"),
                new ClassPathResource("mapper/TranslationsMapper.xml"),
                new ClassPathResource("mapper/ConfigImageImportMapper.xml"));
        org.apache.ibatis.session.Configuration mybatis = new org.apache.ibatis.session.Configuration();
        mybatis.setDefaultStatementTimeout(10);
        factory.setConfiguration(mybatis);
        factory.afterPropertiesSet();
        SqlSessionTemplate sessions = new SqlSessionTemplate(factory.getObject());
        PermissionService permissions = mock(PermissionService.class);
        when(permissions.hasPermi(anyString())).thenReturn(true);
        storage = mock(ConfigImageImportStorage.class);

        context = new AnnotationConfigApplicationContext();
        context.register(Transactions.class);
        context.registerBean(DataSource.class, () -> dataSource);
        context.registerBean(GoodsMemberLevelMapper.class, () -> sessions.getMapper(GoodsMemberLevelMapper.class));
        context.registerBean(TranslationsMapper.class, () -> sessions.getMapper(TranslationsMapper.class));
        context.registerBean(TranslationsServiceImpl.class);
        context.registerBean(GoodsMemberLevelServiceImpl.class);
        context.registerBean(OrderConfigServiceImpl.class, () -> new OrderConfigServiceImpl(
                sessions.getMapper(OrderConfigMapper.class), JSON, context.getBean(ITranslationsService.class)));
        context.registerBean(ConfigImageImportService.class, () -> new ConfigImageImportService(
                context.getBean(IGoodsMemberLevelService.class),
                observeRealConfigService(context.getBean(IOrderConfigService.class)),
                permissions, JSON, storage, sessions.getMapper(ConfigImageImportMapper.class)));
        context.refresh();
        service = context.getBean(ConfigImageImportService.class);
    }

    private static IOrderConfigService observeRealConfigService(IOrderConfigService real) {
        return (IOrderConfigService) Proxy.newProxyInstance(IOrderConfigService.class.getClassLoader(),
                new Class<?>[] { IOrderConfigService.class }, (proxy, method, arguments) -> {
                    Object result;
                    try {
                        result = method.invoke(real, arguments);
                    } catch (InvocationTargetException exception) {
                        throw exception.getCause();
                    }
                    if (method.getName().equals("selectOrderConfigByTypes")) {
                        CyclicBarrier barrier = PLAN_BARRIER.get();
                        // Both requests establish their normal-read snapshots before either locking read.
                        if (barrier != null) barrier.await(10, TimeUnit.SECONDS);
                    }
                    if (method.getName().equals("updateOrderConfig")
                            && ((OrderConfig) arguments[0]).getId().equals(20L)
                            && Integer.valueOf(1).equals(result)) {
                        EVENT_WRITES.incrementAndGet();
                    }
                    return result;
                });
    }

    @AfterAll
    static void closeTheTestContext() {
        if (context != null) context.close();
    }

    @BeforeEach
    void createOnlyOwnedTablesAndTemporaryStorage() throws Exception {
        PLAN_BARRIER.set(null);
        EVENT_WRITES.set(0);
        for (String table : List.of("goods_member_level", "order_config", "translations")) {
            jdbc.execute("DROP TABLE IF EXISTS " + table);
        }
        jdbc.execute("""
                CREATE TABLE translations (
                    id BIGINT PRIMARY KEY, zh_CN TEXT, zh_TW TEXT, ko_KR TEXT, th_TH TEXT,
                    ja_JP TEXT, pt_PT TEXT, en_US TEXT, ar_SA TEXT, es_ES TEXT, sv_SE TEXT,
                    it_IT TEXT, de_DE TEXT, no_NO TEXT, ru_RU TEXT, hu_HU TEXT, pl_PL TEXT,
                    sk_SK TEXT, fr_FR TEXT, cs_CZ TEXT, pt_BR TEXT, hi_IN TEXT, relative_content LONGTEXT
                ) ENGINE=InnoDB
                """);
        jdbc.execute("""
                CREATE TABLE order_config (
                    id BIGINT PRIMARY KEY, type VARCHAR(50) NOT NULL UNIQUE, sort BIGINT,
                    name VARCHAR(100), content TEXT, content_relative LONGTEXT, translations_id BIGINT,
                    create_time DATETIME(3), update_time DATETIME(3),
                    FOREIGN KEY (translations_id) REFERENCES translations(id)
                ) ENGINE=InnoDB
                """);
        jdbc.execute("""
                CREATE TABLE goods_member_level (
                    id BIGINT PRIMARY KEY, name VARCHAR(255), level INT, icon VARCHAR(500),
                    price DECIMAL(20,2), min_balance DECIMAL(20,2), invite_count BIGINT,
                    order_count_per_day BIGINT, min_commission_rate DECIMAL(5,2),
                    max_commission_rate DECIMAL(5,2), min_continuous_commission_rate DECIMAL(5,2),
                    max_continuous_commission_rate DECIMAL(5,2), task_count_per_day BIGINT,
                    withdraw_count_per_day BIGINT, withdraw_fee_rate DECIMAL(5,2),
                    min_withdraw_amount DECIMAL(20,2), withdraw_limit_per_day DECIMAL(20,2),
                    min_withdraw DECIMAL(20,2), max_withdraw DECIMAL(20,2), description TEXT, description_relative LONGTEXT,
                    create_time DATETIME(3), product_match_enabled CHAR(1),
                    product_match_min DECIMAL(5,2), product_match_max DECIMAL(5,2), translations_id BIGINT,
                    FOREIGN KEY (translations_id) REFERENCES translations(id)
                ) ENGINE=InnoDB
                """);
        for (long id : List.of(101L, 102L, 201L, 202L)) {
            jdbc.update("INSERT INTO translations(id,en_US,es_ES,hi_IN) VALUES (?,?,?,?)",
                    id, "<p>English " + id + "</p>", "<p>Spanish " + id + "</p>", "<p>Hidden language</p>");
        }
        jdbc.update("""
                INSERT INTO goods_member_level
                    (id,name,level,icon,price,min_balance,invite_count,order_count_per_day,
                     min_commission_rate,max_commission_rate,description,product_match_enabled,
                     product_match_min,product_match_max,translations_id)
                VALUES (901,'VIP one',1,'/original-one.png',100,50,3,40,1,2,'Original description','1',30,60,101),
                       (102,'VIP two',2,'/original-two.png',500,100,6,80,2,3,'Second description','0',40,70,102)
                """);
        jdbc.update("INSERT INTO order_config(id,type,sort,name,content,translations_id) VALUES (20,'event',2,'Event',?,201)",
                "{\"eventContent\":\"<p>Original event</p>\",\"extension\":{\"keep\":7}}");
        jdbc.update("INSERT INTO order_config(id,type,sort,name,content,translations_id) VALUES (30,'certificate',3,'Certificate',?,202)",
                "{\"certificateContent\":\"<p>Original certificate</p>\",\"certificate\":\"old alias\","
                        + "\"content\":\"older alias\",\"certificateSwitch\":true,\"certificateTip\":\"Keep notice\","
                        + "\"extra\":{\"keep\":8},\"value\":{\"certificate\":\"wrapped alias\","
                        + "\"certificateTip\":\"Keep wrapped notice\",\"other\":9}}");
        jdbc.update("INSERT INTO order_config(id,type,sort,name,content) VALUES (10,'website',1,'Website',?)",
                "{\"certificateSwitch\":\"0\",\"name\":\"Original site\"}");

        reset(storage);
        when(storage.store(anyString(), anyString(), any(), anyString(), anyLong())).thenAnswer(call -> {
            String original = call.getArgument(0);
            String basename = original.substring(original.lastIndexOf('/') + 1);
            String extension = call.getArgument(1);
            Path path = Files.createTempFile(files, basename.substring(0, basename.lastIndexOf('.')) + "-", "." + extension);
            Files.write(path, call.<byte[]>getArgument(2));
            writtenFiles.add(path);
            return new ConfigImageImportStorage.StoredImage("https://images.test/" + path.getFileName(), path);
        });
        doAnswer(call -> { Files.deleteIfExists(call.getArgument(0)); return null; }).when(storage).delete(any());
    }

    @Test
    void importsVipAndOrderedRichTextWhilePreservingOtherConfiguration() throws Exception {
        Map<String, Object> originalVip = jdbc.queryForMap("SELECT * FROM goods_member_level WHERE id=901");
        Map<String, Object> untouchedVip = jdbc.queryForMap("SELECT * FROM goods_member_level WHERE id=102");
        List<Map<String, Object>> translations = translationRows();
        List<Map<String, Object>> metadata = configMetadata();
        String website = content("website");

        var result = service.importImages(zip("v01.png", "s10.png", "s02.png", "z03.png", "z01.png"), "append");

        assertTrue(result.valid(), result.errors().toString());
        Map<String, Object> changedVip = jdbc.queryForMap("SELECT * FROM goods_member_level WHERE id=901");
        assertTrue(changedVip.get("icon").toString().contains("v01-"));
        originalVip.remove("icon");
        changedVip.remove("icon");
        assertEquals(originalVip, changedVip);
        assertEquals(untouchedVip, jdbc.queryForMap("SELECT * FROM goods_member_level WHERE id=102"));

        JsonNode event = JSON.readTree(content("event"));
        assertOrderedImages(event.path("eventContent").asText(), "<p>Original event</p>", "s02-", "s10-");
        assertEquals(7, event.path("extension").path("keep").asInt());
        JsonNode certificate = JSON.readTree(content("certificate"));
        String html = certificate.path("certificateContent").asText();
        assertOrderedImages(html, "<p>Original certificate</p>", "z01-", "z03-");
        assertEquals(html, certificate.path("certificate").asText());
        assertEquals(html, certificate.path("content").asText());
        assertEquals(html, certificate.path("value").path("certificateContent").asText());
        assertEquals(html, certificate.path("value").path("certificate").asText());
        assertTrue(certificate.path("certificateSwitch").asBoolean());
        assertEquals("Keep notice", certificate.path("certificateTip").asText());
        assertEquals("Keep wrapped notice", certificate.path("value").path("certificateTip").asText());
        assertEquals(8, certificate.path("extra").path("keep").asInt());
        assertEquals(9, certificate.path("value").path("other").asInt());
        assertEquals(metadata, configMetadata());
        assertEquals(translations, translationRows());
        assertEquals(website, content("website"));
        assertEquals(5, writtenFiles.size());
        assertTrue(writtenFiles.stream().allMatch(Files::exists));
        verify(storage, never()).delete(any());
    }

    @Test
    void aLaterDatabaseSaveFailureRollsBackTheWholePackageAndRemovesItsFiles() throws Exception {
        List<Map<String, Object>> originalLevels = jdbc.queryForList("SELECT * FROM goods_member_level ORDER BY id");
        List<Map<String, Object>> originalConfigs = jdbc.queryForList("SELECT * FROM order_config ORDER BY id");
        List<Map<String, Object>> translations = translationRows();
        jdbc.execute("""
                CREATE TRIGGER reject_certificate_import BEFORE UPDATE ON order_config FOR EACH ROW
                BEGIN
                    IF NEW.type = 'certificate' THEN
                        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Forced isolated import save failure';
                    END IF;
                END
                """);

        assertThrows(RuntimeException.class,
                () -> service.importImages(zip("v01.png", "s01.png", "z01.png"), "append"));

        assertEquals(1, EVENT_WRITES.get(), "The real event UPDATE must succeed before the certificate save fails");
        assertEquals(originalLevels, jdbc.queryForList("SELECT * FROM goods_member_level ORDER BY id"));
        assertEquals(originalConfigs, jdbc.queryForList("SELECT * FROM order_config ORDER BY id"));
        assertEquals(translations, translationRows());
        assertEquals(3, writtenFiles.size());
        assertTrue(writtenFiles.stream().noneMatch(Files::exists));
    }

    @Test
    void concurrentAppendsKeepBothBatchesAfterBothRequestsReadTheOriginalContent() throws Exception {
        PLAN_BARRIER.set(new CyclicBarrier(2));
        var workers = Executors.newFixedThreadPool(2);
        try {
            MockMultipartFile firstZip = zip("s01.png", "z01.png");
            MockMultipartFile secondZip = zip("s02.png", "z02.png");
            var first = workers.submit(() -> service.importImages(firstZip, "append"));
            var second = workers.submit(() -> service.importImages(secondZip, "append"));
            assertTrue(first.get(15, TimeUnit.SECONDS).valid());
            assertTrue(second.get(15, TimeUnit.SECONDS).valid());
        } finally {
            PLAN_BARRIER.set(null);
            workers.shutdownNow();
            assertTrue(workers.awaitTermination(5, TimeUnit.SECONDS));
        }
        JsonNode event = JSON.readTree(content("event"));
        String eventHtml = event.path("eventContent").asText();
        assertTrue(eventHtml.startsWith("<p>Original event</p>"));
        assertTrue(eventHtml.contains("s01-"));
        assertTrue(eventHtml.contains("s02-"));
        assertEquals(2, eventHtml.split("<img", -1).length - 1);
        JsonNode certificate = JSON.readTree(content("certificate"));
        String certificateHtml = certificate.path("certificateContent").asText();
        assertTrue(certificateHtml.startsWith("<p>Original certificate</p>"));
        assertTrue(certificateHtml.contains("z01-"));
        assertTrue(certificateHtml.contains("z02-"));
        assertEquals(2, certificateHtml.split("<img", -1).length - 1);
        assertTrue(certificate.path("certificateSwitch").asBoolean());
        assertEquals(2, EVENT_WRITES.get());
        assertEquals(4, writtenFiles.size());
        assertTrue(writtenFiles.stream().allMatch(Files::exists));
    }

    private static String content(String type) {
        return jdbc.queryForObject("SELECT content FROM order_config WHERE type=?", String.class, type);
    }

    private static List<Map<String, Object>> translationRows() {
        return jdbc.queryForList("SELECT * FROM translations ORDER BY id");
    }

    private static List<Map<String, Object>> configMetadata() {
        return jdbc.queryForList("SELECT id,type,sort,name,translations_id FROM order_config ORDER BY id");
    }

    private static void assertOrderedImages(String html, String original, String first, String second) {
        assertTrue(html.startsWith(original));
        assertTrue(html.contains(first));
        assertTrue(html.indexOf(second) > html.indexOf(first));
        assertEquals(2, html.split("<img", -1).length - 1);
    }

    private static MockMultipartFile zip(String... names) throws Exception {
        ByteArrayOutputStream imageBytes = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", imageBytes));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (String name : names) {
                zip.putNextEntry(new ZipEntry(name));
                zip.write(imageBytes.toByteArray());
                zip.closeEntry();
            }
        }
        return new MockMultipartFile("file", "images.zip", "application/zip", bytes.toByteArray());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    static class Transactions {
        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
    }
}
