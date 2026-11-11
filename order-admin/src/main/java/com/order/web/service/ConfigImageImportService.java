package com.order.web.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.order.framework.web.service.PermissionService;
import com.order.member.domain.GoodsMemberLevel;
import com.order.member.domain.OrderConfig;
import com.order.member.mapper.ConfigImageImportMapper;
import com.order.member.service.IGoodsMemberLevelService;
import com.order.member.service.IOrderConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.CRC32;

@Service
public class ConfigImageImportService {
    public static final long MAX_ZIP_BYTES = 10L * 1024 * 1024;
    static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    static final long MAX_EXPANDED_BYTES = 100L * 1024 * 1024;
    static final int MAX_ENTRIES = 300;
    private static final long MAX_PIXELS = 20_000_000;
    private static final Pattern IMAGE_NAME = Pattern.compile(
            "^([vsz])(\\d{2,})\\.(jpg|jpeg|png|webp|gif|bmp)$", Pattern.CASE_INSENSITIVE);

    private final IGoodsMemberLevelService levelService;
    private final IOrderConfigService configService;
    private final PermissionService permissions;
    private final ObjectMapper objectMapper;
    private final ConfigImageImportStorage storage;
    private final ConfigImageImportMapper importMapper;

    public ConfigImageImportService(IGoodsMemberLevelService levelService, IOrderConfigService configService,
            PermissionService permissions, ObjectMapper objectMapper, ConfigImageImportStorage storage,
            ConfigImageImportMapper importMapper) {
        this.levelService = levelService;
        this.configService = configService;
        this.permissions = permissions;
        this.objectMapper = objectMapper;
        this.storage = storage;
        this.importMapper = importMapper;
    }

    public ImportResult preview(MultipartFile file, String richTextMode) {
        return plan(file, richTextMode).result();
    }

    // A request may wait for another import's row lock after planning. READ_COMMITTED lets
    // shared-file deduplication see that import's newly committed hash rather than an old RR snapshot.
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public ImportResult importImages(MultipartFile file, String richTextMode) {
        Plan plan = plan(file, richTextMode);
        if (!plan.result().valid()) {
            return plan.result();
        }
        // Fixed lock order and current reads preserve concurrent appended batches even under MySQL RR.
        Map<String, OrderConfig> lockedConfigs = new LinkedHashMap<>();
        for (String group : List.of("event", "certificate")) {
            Image first = plan.images.stream().filter(image -> group.equals(image.group)).findFirst().orElse(null);
            if (first == null) continue;
            OrderConfig target = importMapper.selectByTypeForUpdate(group);
            if (target == null || !first.targetId.equals(target.getId())) {
                throw new IllegalStateException("配置已变化，请重新预览导入");
            }
            contentObject(target.getContent(), group);
            lockedConfigs.put(group, target);
        }
        List<Path> generated = new ArrayList<>();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        generated.forEach(storage::delete);
                    }
                }
            });
        }
        Map<String, StringBuilder> html = new HashMap<>();
        try {
            for (Image image : plan.images) {
                ConfigImageImportStorage.StoredImage stored = storage.store(image.fileName, image.extension,
                        image.bytes, image.group, image.targetId);
                generated.add(stored.writtenPath());
                if ("vip".equals(image.group)) {
                    GoodsMemberLevel patch = new GoodsMemberLevel();
                    patch.setId(image.targetId);
                    patch.setIcon(stored.url());
                    if (levelService.updateGoodsMemberLevel(patch) != 1) {
                        throw new IllegalStateException("VIP 等级已变化，请重新预览导入");
                    }
                } else {
                    html.computeIfAbsent(image.group, ignored -> new StringBuilder())
                            .append("<p><img src=\"").append(escapeAttribute(stored.url()))
                            .append("\" style=\"max-width:100%;\" /></p>");
                }
            }
            for (Map.Entry<String, OrderConfig> item : lockedConfigs.entrySet()) {
                OrderConfig target = item.getValue();
                OrderConfig patch = new OrderConfig();
                patch.setId(target.getId());
                patch.setContent(mergeContent(target.getContent(), item.getKey(), html.get(item.getKey()).toString(), richTextMode));
                if (configService.updateOrderConfig(patch) != 1) {
                    throw new IllegalStateException("配置已变化，请重新预览导入");
                }
            }
            return plan.result();
        } catch (RuntimeException exception) {
            generated.forEach(storage::delete);
            throw exception;
        }
    }

    private Plan plan(MultipartFile file, String mode) {
        Plan plan = new Plan();
        if (!"append".equals(mode) && !"replace".equals(mode)) {
            plan.errors.add("请选择追加或替换导入方式");
            return plan;
        }
        if (file == null || file.isEmpty() || file.getSize() > MAX_ZIP_BYTES) {
            plan.errors.add("请选择非空 ZIP 文件，压缩包不能超过 10MB");
            return plan;
        }
        if (file.getOriginalFilename() == null
                || !file.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".zip")) {
            plan.errors.add("仅支持 ZIP 压缩包");
            return plan;
        }
        readZip(file, plan);
        if (plan.images.isEmpty()) {
            plan.errors.add("压缩包内没有符合命名规则的图片");
        }
        resolveTargets(plan);
        plan.images.sort(Comparator.comparing((Image image) -> image.group).thenComparingLong(image -> image.sequence));
        return plan;
    }

    private void readZip(MultipartFile file, Plan plan) {
        long totalBytes = 0;
        int entryCount = 0;
        Set<String> keys = new HashSet<>();
        // No archive path is ever used as an output path; images receive generated storage names.
        Path archive = null;
        try {
            archive = Files.createTempFile("config-image-import-", ".zip");
            try (var input = file.getInputStream(); var output = Files.newOutputStream(archive)) {
                byte[] buffer = new byte[8192];
                long copied = 0;
                int count;
                while ((count = input.read(buffer)) != -1) {
                    copied += count;
                    if (copied > MAX_ZIP_BYTES) {
                        plan.errors.add("压缩包不能超过 10MB");
                        return;
                    }
                    output.write(buffer, 0, count);
                }
            }
            // ZipFile requires an intact central directory/EOCD, unlike ZipInputStream.
            try (ZipFile zip = new ZipFile(archive.toFile(), StandardCharsets.UTF_8)) {
                var entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (++entryCount > MAX_ENTRIES) {
                        plan.errors.add("压缩包最多包含 300 个文件或目录");
                        return;
                    }
                    String name = entry.getName().replace('\\', '/');
                    if (!safePath(name)) {
                        plan.errors.add("压缩包包含不安全的文件路径：" + printable(name));
                        return;
                    }
                    String baseName = name.substring(name.lastIndexOf('/') + 1);
                    if (baseName.length() > 255) {
                        plan.errors.add("图片文件名不能超过 255 个字符：" + printable(name));
                        return;
                    }
                    boolean ignored = entry.isDirectory() || hiddenMetadata(name, baseName);
                    Matcher matcher = IMAGE_NAME.matcher(baseName);
                    boolean named = !ignored && matcher.matches();
                    if (!ignored && !named) {
                        plan.errors.add("文件名或图片格式不符合规则：" + printable(name)
                                + "（示例 v01.png、s01.webp、z01.jpeg）");
                    }
                    ByteArrayOutputStream bytes = named ? new ByteArrayOutputStream() : null;
                    long imageBytes = 0;
                    CRC32 crc = new CRC32();
                    byte[] buffer = new byte[8192];
                    int read;
                    try (var member = zip.getInputStream(entry)) {
                        while ((read = member.read(buffer)) != -1) {
                            totalBytes += read;
                            imageBytes += read;
                            crc.update(buffer, 0, read);
                            if (totalBytes > MAX_EXPANDED_BYTES) {
                                plan.errors.add("解压后的文件总大小不能超过 100MB");
                                return;
                            }
                            if (imageBytes > MAX_IMAGE_BYTES) {
                                plan.errors.add("单个文件不能超过 10MB：" + printable(name));
                                return;
                            }
                            if (bytes != null) {
                                bytes.write(buffer, 0, read);
                            }
                        }
                    }
                    if (entry.getCrc() != crc.getValue() || entry.getSize() != imageBytes) {
                        plan.errors.add("压缩包内文件损坏：" + printable(name));
                        return;
                    }
                    if (!named) {
                        continue;
                    }
                    long sequence;
                    try {
                        sequence = Long.parseLong(matcher.group(2));
                        if (sequence < 1) {
                            throw new NumberFormatException();
                        }
                    } catch (NumberFormatException exception) {
                        plan.errors.add("图片编号必须为正整数：" + printable(name));
                        continue;
                    }
                    String prefix = matcher.group(1).toLowerCase(Locale.ROOT);
                    String key = prefix + sequence;
                    if (!keys.add(key)) {
                        plan.errors.add("同一位置的图片重复：" + printable(name));
                    }
                    String extension = matcher.group(3).toLowerCase(Locale.ROOT);
                    byte[] imageBytesValue = bytes.toByteArray();
                    if (!validImage(imageBytesValue, extension)) {
                        plan.errors.add("图片内容无效、格式与后缀不符或尺寸过大：" + printable(name));
                        continue;
                    }
                    String group = switch (prefix) {
                        case "v" -> "vip";
                        case "s" -> "event";
                        default -> "certificate";
                    };
                    plan.images.add(new Image(name, group, sequence, extension, imageBytesValue));
                }
            }
        } catch (IOException | IllegalArgumentException exception) {
            plan.errors.add("无法读取 ZIP，请确认压缩包完整、未加密且使用标准 ZIP 格式");
        } finally {
            if (archive != null) {
                try {
                    Files.deleteIfExists(archive);
                } catch (IOException exception) {
                    org.slf4j.LoggerFactory.getLogger(getClass()).warn("Unable to remove temporary image archive", exception);
                }
            }
        }
    }

    private void resolveTargets(Plan plan) {
        Set<String> groups = new HashSet<>();
        plan.images.forEach(image -> groups.add(image.group));
        if (groups.contains("vip")) {
            if (!permissions.hasPermi("member:level:edit")) {
                plan.errors.add("没有修改 VIP 等级图片的权限");
            } else {
                Map<Long, List<GoodsMemberLevel>> byLevel = new HashMap<>();
                for (GoodsMemberLevel level : levelService.selectGoodsMemberLevelList(new GoodsMemberLevel())) {
                    byLevel.computeIfAbsent(level.getLevel(), ignored -> new ArrayList<>()).add(level);
                }
                for (Image image : plan.images) {
                    if (!"vip".equals(image.group)) {
                        continue;
                    }
                    List<GoodsMemberLevel> matches = byLevel.getOrDefault(image.sequence, List.of());
                    image.targetLabel = "VIP " + image.sequence;
                    if (matches.size() != 1) {
                        plan.errors.add(image.fileName + "：VIP " + image.sequence
                                + (matches.isEmpty() ? " 等级不存在" : " 存在多条记录，无法唯一匹配"));
                    } else {
                        image.targetId = matches.get(0).getId();
                        image.targetLabel += "（" + matches.get(0).getName() + "）";
                    }
                }
            }
        }
        if (groups.contains("event") || groups.contains("certificate")) {
            if (!permissions.hasPermi("member:orderconfig:edit")) {
                plan.errors.add("没有修改事件或证书图片的权限");
                return;
            }
            Map<String, List<OrderConfig>> configs = new HashMap<>();
            for (OrderConfig config : configService.selectOrderConfigByTypes(List.of("event", "certificate"))) {
                configs.computeIfAbsent(config.getType(), ignored -> new ArrayList<>()).add(config);
            }
            for (String group : List.of("event", "certificate")) {
                if (!groups.contains(group)) {
                    continue;
                }
                String label = "event".equals(group) ? "事件" : "证书";
                List<OrderConfig> matches = configs.getOrDefault(group, List.of());
                if (matches.size() != 1) {
                    plan.errors.add(label + (matches.isEmpty() ? "配置不存在，请先创建配置" : "配置存在多条，无法唯一匹配"));
                    continue;
                }
                try {
                    contentObject(matches.get(0).getContent(), group);
                } catch (IllegalArgumentException exception) {
                    plan.errors.add(label + "配置内容无法解析，请先检查并保存该配置");
                }
                for (Image image : plan.images) {
                    if (group.equals(image.group)) {
                        image.targetId = matches.get(0).getId();
                        image.targetLabel = label + " · 第 " + image.sequence + " 张";
                    }
                }
            }
        }
    }

    private String mergeContent(String source, String group, String images, String mode) {
        ObjectNode root = contentObject(source, group);
        String field = "event".equals(group) ? "eventContent" : "certificateContent";
        List<String> aliases = "event".equals(group)
                ? List.of(field, "event", "content", "value") : List.of(field, "certificate", "content", "value");
        String current = null;
        List<ObjectNode> containers = new ArrayList<>(List.of(root));
        if (root.path("value").isObject()) {
            containers.add((ObjectNode) root.get("value"));
        }
        for (String alias : aliases) {
            for (ObjectNode container : containers) {
                JsonNode value = container.get(alias);
                if (current == null && value != null && value.isTextual() && !value.asText().isBlank()) {
                    current = value.asText();
                }
            }
        }
        String merged = ("append".equals(mode) && current != null ? current : "") + images;
        root.put(field, merged);
        for (ObjectNode container : containers) {
            if (container != root) {
                container.put(field, merged);
            }
            for (String alias : aliases) {
                if (container.path(alias).isTextual()) {
                    container.put(alias, merged);
                }
            }
        }
        return root.toString();
    }

    private ObjectNode contentObject(String source, String group) {
        if (source == null || source.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            JsonNode value = objectMapper.readTree(source);
            if (value != null && value.isObject()) {
                return ((ObjectNode) value).deepCopy();
            }
            if (value != null && value.isTextual()) {
                return objectMapper.createObjectNode().put(contentField(group), value.asText());
            }
        } catch (IOException exception) {
            if (!source.stripLeading().startsWith("{") && !source.stripLeading().startsWith("[")) {
                return objectMapper.createObjectNode().put(contentField(group), source);
            }
        }
        throw new IllegalArgumentException("配置内容无法解析");
    }

    private String contentField(String group) {
        return "event".equals(group) ? "eventContent" : "certificateContent";
    }

    private boolean validImage(byte[] data, String extension) {
        if (data.length == 0) {
            return false;
        }
        if ("gif".equals(extension)) {
            if (data.length < 10) return false;
            long canvasWidth = (data[6] & 0xff) | ((data[7] & 0xff) << 8);
            long canvasHeight = (data[8] & 0xff) | ((data[9] & 0xff) << 8);
            if (canvasWidth == 0 || canvasHeight == 0 || canvasWidth * canvasHeight > MAX_PIXELS) return false;
        }
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                return false;
            }
            ImageReader reader = readers.next();
            try {
                String expected = "jpg".equals(extension) ? "jpeg" : extension;
                if (!expected.equalsIgnoreCase(reader.getFormatName())) {
                    return false;
                }
                reader.setInput(input, true, true);
                long width = reader.getWidth(0);
                long height = reader.getHeight(0);
                return width > 0 && height > 0 && width * height <= MAX_PIXELS && reader.read(0) != null;
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }

    private boolean safePath(String name) {
        if (name.isEmpty() || name.length() > 1024 || name.startsWith("/") || name.indexOf(':') >= 0
                || name.indexOf('\0') >= 0) return false;
        for (String part : name.split("/")) {
            if ("..".equals(part) || ".".equals(part)) return false;
        }
        return true;
    }

    private boolean hiddenMetadata(String name, String baseName) {
        for (String part : name.split("/")) {
            if (part.startsWith(".") || "__MACOSX".equalsIgnoreCase(part)) return true;
        }
        return "Thumbs.db".equalsIgnoreCase(baseName);
    }

    private String printable(String name) {
        return name.replaceAll("[\\p{Cntrl}]", "?").substring(0, Math.min(name.length(), 180));
    }

    private String escapeAttribute(String value) {
        return value.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public record ImportEntry(String fileName, String group, long sequence, Long targetId, String targetLabel) {
    }

    public record ImportResult(List<ImportEntry> entries, Map<String, Integer> summary, List<String> errors, boolean valid) {
    }

    private static final class Image {
        final String fileName;
        final String group;
        final long sequence;
        final String extension;
        final byte[] bytes;
        Long targetId;
        String targetLabel;

        Image(String fileName, String group, long sequence, String extension, byte[] bytes) {
            this.fileName = fileName;
            this.group = group;
            this.sequence = sequence;
            this.extension = extension;
            this.bytes = bytes;
        }
    }

    private static final class Plan {
        final List<Image> images = new ArrayList<>();
        final List<String> errors = new ArrayList<>();

        ImportResult result() {
            Map<String, Integer> summary = new LinkedHashMap<>();
            summary.put("vip", 0);
            summary.put("event", 0);
            summary.put("certificate", 0);
            for (Image image : images) summary.compute(image.group, (group, count) -> count + 1);
            List<ImportEntry> entries = images.stream().map(image -> new ImportEntry(image.fileName,
                    image.group, image.sequence, image.targetId, image.targetLabel)).toList();
            return new ImportResult(entries, summary, List.copyOf(errors), errors.isEmpty() && !images.isEmpty());
        }
    }
}
