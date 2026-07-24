package com.order.common.utils.file;

import org.apache.commons.io.FilenameUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Validates public image uploads using the declared MIME type and decoded image format.
 */
public final class ImageUploadValidator {
    public static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024;

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("png", "jpg", "jpeg", "gif", "bmp");
    private static final Map<String, Set<String>> ALLOWED_MIME_TYPES = Map.of(
            "png", Set.of("image/png"),
            "jpg", Set.of("image/jpg", "image/jpeg"),
            "jpeg", Set.of("image/jpg", "image/jpeg"),
            "gif", Set.of("image/gif"),
            "bmp", Set.of("image/bmp"));

    private ImageUploadValidator() {
    }

    public static void validate(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_IMAGE_SIZE) {
            throw new InvalidImageUploadException();
        }
        String originalFilename = file.getOriginalFilename();
        String extension = FilenameUtils.getExtension(originalFilename);
        extension = extension == null ? "" : extension.toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new InvalidImageUploadException();
        }
        String contentType = file.getContentType();
        contentType = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (!ALLOWED_MIME_TYPES.get(extension).contains(contentType)) {
            throw new InvalidImageUploadException();
        }

        try (InputStream input = file.getInputStream();
             ImageInputStream imageInput = ImageIO.createImageInputStream(input)) {
            if (imageInput == null) {
                throw new InvalidImageUploadException();
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw new InvalidImageUploadException();
            }
            ImageReader reader = readers.next();
            try {
                String decodedFormat = normalizeFormat(reader.getFormatName());
                if (!normalizeFormat(extension).equals(decodedFormat)) {
                    throw new InvalidImageUploadException();
                }
                reader.setInput(imageInput, true, true);
                BufferedImage decoded = reader.read(0);
                if (decoded == null || decoded.getWidth() <= 0 || decoded.getHeight() <= 0) {
                    throw new InvalidImageUploadException();
                }
            } finally {
                reader.dispose();
            }
        } catch (InvalidImageUploadException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new InvalidImageUploadException(exception);
        }
    }

    private static String normalizeFormat(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return "jpg".equals(normalized) ? "jpeg" : normalized;
    }

    public static final class InvalidImageUploadException extends RuntimeException {
        public InvalidImageUploadException() {
            super("Invalid image upload");
        }

        public InvalidImageUploadException(Throwable cause) {
            super("Invalid image upload", cause);
        }
    }
}
