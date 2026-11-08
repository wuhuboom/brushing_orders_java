package com.order.common.utils.poi;

import com.order.common.annotation.Excel;
import com.order.common.annotation.Excel.ColumnType;
import com.order.common.exception.UtilException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipFile;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.util.DefaultTempFileCreationStrategy;
import org.apache.poi.util.TempFile;
import org.apache.poi.util.TempFileCreationStrategy;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class ExcelUtilBatchExportTest {
    @TempDir Path directory;
    private TrackingTempFiles temporaryFiles;

    @BeforeEach
    void isolatePoiTemporaryFiles() {
        temporaryFiles = new TrackingTempFiles(directory.resolve("poi"));
        TempFile.setTempFileCreationStrategy(temporaryFiles);
    }

    @AfterEach
    void restorePoiTemporaryFiles() {
        TempFile.setTempFileCreationStrategy(new DefaultTempFileCreationStrategy());
    }

    @Test
    void lazyBatchesFlushRowsAndRollSheetsWithoutRetainingTheDataset() throws Exception {
        ExcelUtil<ExportRow> util = new ExcelUtil<>(ExportRow.class);
        AtomicInteger produced = new AtomicInteger();
        AtomicInteger calls = new AtomicInteger();
        int total = ExcelUtil.sheetSize + 7;
        Path output = directory.resolve("large.xlsx");
        try (ResponseCapture response = new ResponseCapture(Files.newOutputStream(output))) {
            util.exportExcelInBatches(response.response, "会员核心数据", () -> {
                assertOnlyTheCurrentBatchCanBeRetained(util, calls.getAndIncrement() > 0);
                int count = Math.min(500, total - produced.get());
                List<ExportRow> batch = new ArrayList<>(count);
                for (int index = 0; index < count; index++) {
                    batch.add(new ExportRow(produced.incrementAndGet()));
                }
                return batch;
            });
            assertEquals(1, response.outputRequests);
            assertTrue(response.contentType.startsWith("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        }
        assertEquals(total, produced.get());
        assertTrue(calls.get() > 100, "Dataset must be supplied lazily in many batches");
        try (ZipFile zip = new ZipFile(output.toFile())) {
            SheetSummary first = summarize(zip, "xl/worksheets/sheet1.xml");
            SheetSummary second = summarize(zip, "xl/worksheets/sheet2.xml");
            assertEquals(ExcelUtil.sheetSize + 1, first.rows);
            assertEquals(8, second.rows);
            assertEquals("inlineStr", first.firstIdType);
            assertEquals("009007199254740994", first.firstId);
            assertEquals("009007199254806530", second.firstId);
            assertEquals(0, new BigDecimal(total).multiply(new BigDecimal(total + 1))
                    .divide(new BigDecimal("2")).add(new BigDecimal("0.25").multiply(new BigDecimal(total)))
                    .compareTo(first.amount.add(second.amount)));
            assertTrue(first.frozenHeader && second.frozenHeader);
            assertNull(zip.getEntry("xl/worksheets/sheet3.xml"));
        }
        assertTemporaryFilesDisposed();
    }

    @Test
    void emptyDatasetProducesHeadersAndCleansTemporaryFiles() throws Exception {
        Path output = directory.resolve("empty.xlsx");
        try (ResponseCapture response = new ResponseCapture(Files.newOutputStream(output))) {
            new ExcelUtil<>(ExportRow.class).exportExcelInBatches(response.response, "会员核心数据", List::of);
        }
        try (ZipFile zip = new ZipFile(output.toFile())) {
            assertEquals(1, summarize(zip, "xl/worksheets/sheet1.xml").rows);
        }
        assertTemporaryFilesDisposed();
    }

    @Test
    void sourceFailurePropagatesBeforeResponseAndDisposesTemporaryFiles() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        IllegalStateException failure = new IllegalStateException("source failed");
        try (ResponseCapture response = new ResponseCapture(OutputStream.nullOutputStream())) {
            IllegalStateException thrown = assertThrows(IllegalStateException.class,
                    () -> new ExcelUtil<>(ExportRow.class).exportExcelInBatches(response.response, "会员核心数据", () -> {
                        if (calls.incrementAndGet() == 3) throw failure;
                        return List.of(new ExportRow(calls.get()));
                    }));
            assertSame(failure, thrown);
            assertEquals(0, response.outputRequests, "No partial success file may be returned for a query failure");
        }
        assertTemporaryFilesDisposed();
    }

    @Test
    void invalidRowFailsInsteadOfSilentlyReturningAnIncompleteWorkbook() throws Exception {
        try (ResponseCapture response = new ResponseCapture(OutputStream.nullOutputStream())) {
            assertThrows(UtilException.class, () -> new ExcelUtil<>(ExportRow.class).exportExcelInBatches(
                    response.response, "会员核心数据", () -> Arrays.asList((ExportRow) null)));
            assertEquals(0, response.outputRequests);
        }
        assertTemporaryFilesDisposed();
    }

    @Test
    void clientDisconnectPropagatesAndDisposesTemporaryFiles() throws Exception {
        OutputStream disconnected = new OutputStream() {
            @Override public void write(int value) throws IOException { throw new IOException("client disconnected"); }
        };
        AtomicInteger calls = new AtomicInteger();
        try (ResponseCapture response = new ResponseCapture(disconnected)) {
            assertThrows(IOException.class, () -> new ExcelUtil<>(ExportRow.class).exportExcelInBatches(
                    response.response, "会员核心数据", () -> calls.getAndIncrement() == 0 ? List.of(new ExportRow(1)) : List.of()));
        }
        assertTemporaryFilesDisposed();
    }

    private static void assertOnlyTheCurrentBatchCanBeRetained(ExcelUtil<?> util, boolean afterFirstBatch) {
        try {
            Field rows = ExcelUtil.class.getDeclaredField("list");
            rows.setAccessible(true);
            assertTrue(((List<?>) rows.get(util)).isEmpty(), "The utility must never hold all supplied data");
            Field workbook = ExcelUtil.class.getDeclaredField("wb");
            workbook.setAccessible(true);
            SXSSFWorkbook streaming = (SXSSFWorkbook) workbook.get(util);
            assertEquals(100, streaming.getRandomAccessWindowSize());
            if (afterFirstBatch) {
                for (Sheet sheet : streaming) {
                    assertFalse(sheet.iterator().hasNext(), "Each batch must flush every sheet row window");
                }
            }
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private void assertTemporaryFilesDisposed() {
        assertFalse(temporaryFiles.created.isEmpty(), "Test must observe actual POI temporary files");
        assertTrue(temporaryFiles.created.stream().anyMatch(path -> path.toString().endsWith(".gz")));
        for (Path temporary : temporaryFiles.created) {
            assertFalse(Files.exists(temporary), "Leaked Excel temporary file: " + temporary);
        }
    }

    private static SheetSummary summarize(ZipFile zip, String entry) throws Exception {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
        SheetSummary summary = new SheetSummary();
        try (InputStream input = zip.getInputStream(zip.getEntry(entry))) {
            XMLStreamReader xml = factory.createXMLStreamReader(input);
            try {
                String cell = null;
                String type = null;
                while (xml.hasNext()) {
                    int event = xml.next();
                    if (event == XMLStreamConstants.START_ELEMENT) {
                        switch (xml.getLocalName()) {
                            case "row" -> summary.rows++;
                            case "pane" -> {
                                String split = xml.getAttributeValue(null, "ySplit");
                                summary.frozenHeader = split != null && Double.parseDouble(split) == 1d;
                            }
                            case "c" -> {
                                cell = xml.getAttributeValue(null, "r");
                                type = xml.getAttributeValue(null, "t");
                            }
                            case "t", "v" -> {
                                String value = xml.getElementText();
                                if ("A2".equals(cell)) {
                                    summary.firstId = value;
                                    summary.firstIdType = type;
                                }
                                if (cell != null && cell.startsWith("B") && !"B1".equals(cell)) {
                                    assertEquals("n", type, "Amounts must be numeric Excel cells");
                                    summary.amount = summary.amount.add(new BigDecimal(value));
                                }
                            }
                            default -> { }
                        }
                    } else if (event == XMLStreamConstants.END_ELEMENT && "c".equals(xml.getLocalName())) {
                        cell = null;
                    }
                }
            } finally {
                xml.close();
            }
        }
        return summary;
    }

    private static class SheetSummary {
        int rows;
        BigDecimal amount = BigDecimal.ZERO;
        String firstId;
        String firstIdType;
        boolean frozenHeader;
    }

    private static class ExportRow {
        @Excel(name = "用户ID", sort = 1, cellType = ColumnType.TEXT)
        private final String id;
        @Excel(name = "余额", sort = 2, cellType = ColumnType.NUMERIC, scale = 2)
        private final BigDecimal amount;
        ExportRow(int number) {
            id = "00" + (9007199254740993L + number);
            amount = new BigDecimal(number + ".25");
        }
    }

    private static class TrackingTempFiles implements TempFileCreationStrategy {
        final Path directory;
        final List<Path> created = new ArrayList<>();
        TrackingTempFiles(Path directory) { this.directory = directory; }
        @Override public File createTempFile(String prefix, String suffix) throws IOException {
            Files.createDirectories(directory);
            Path file = Files.createTempFile(directory, prefix, suffix);
            created.add(file);
            return file.toFile();
        }
        @Override public File createTempDirectory(String prefix) throws IOException {
            Files.createDirectories(directory);
            return Files.createTempDirectory(directory, prefix).toFile();
        }
    }

    private static class ResponseCapture implements AutoCloseable {
        final HttpServletResponse response;
        final OutputStream output;
        int outputRequests;
        String contentType;
        ResponseCapture(OutputStream output) {
            this.output = output;
            ServletOutputStream stream = new ServletOutputStream() {
                @Override public boolean isReady() { return true; }
                @Override public void setWriteListener(WriteListener listener) { }
                @Override public void write(int value) throws IOException { output.write(value); }
                @Override public void write(byte[] bytes, int offset, int length) throws IOException { output.write(bytes, offset, length); }
            };
            response = (HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[] { HttpServletResponse.class }, (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "getOutputStream": outputRequests++; return stream;
                            case "setContentType": contentType = (String) args[0]; return null;
                            case "setCharacterEncoding": return null;
                            default: throw new UnsupportedOperationException(method.getName());
                        }
                    });
        }
        @Override public void close() throws IOException { output.close(); }
    }
}
