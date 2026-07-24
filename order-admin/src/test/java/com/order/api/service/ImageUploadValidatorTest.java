package com.order.api.service;

import com.order.common.utils.file.ImageUploadValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImageUploadValidatorTest {

    @ParameterizedTest
    @MethodSource("validImages")
    void acceptsSupportedDecodedImages(String extension, String mimeType, String format)
            throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar." + extension, mimeType, image(format));

        assertDoesNotThrow(() -> ImageUploadValidator.validate(file));
    }

    @Test
    void rejectsEmptyFile() {
        assertInvalid(new MockMultipartFile("file", "empty.png", "image/png", new byte[0]));
    }

    @Test
    void rejectsHtmlEvenWhenNamedAsAnImage() {
        assertInvalid(new MockMultipartFile(
                "file", "payload.png", "image/png", "<html>payload</html>".getBytes()));
    }

    @Test
    void rejectsForgedMimeType() throws Exception {
        assertInvalid(new MockMultipartFile(
                "file", "avatar.png", "image/jpeg", image("png")));
    }

    @Test
    void rejectsExtensionThatDoesNotMatchDecodedFormat() throws Exception {
        assertInvalid(new MockMultipartFile(
                "file", "avatar.jpg", "image/jpeg", image("png")));
    }

    @Test
    void rejectsDamagedImage() {
        assertInvalid(new MockMultipartFile(
                "file", "avatar.gif", "image/gif", new byte[] {'G', 'I', 'F', '8', '9'}));
    }

    @Test
    void rejectsFileLargerThanTenMegabytes() {
        assertInvalid(new MockMultipartFile(
                "file", "avatar.bmp", "image/bmp",
                new byte[(int) ImageUploadValidator.MAX_IMAGE_SIZE + 1]));
    }

    private static Stream<Arguments> validImages() {
        return Stream.of(
                Arguments.of("png", "image/png", "png"),
                Arguments.of("jpg", "image/jpeg", "jpeg"),
                Arguments.of("jpeg", "image/jpeg", "jpeg"),
                Arguments.of("gif", "image/gif", "gif"),
                Arguments.of("bmp", "image/bmp", "bmp"));
    }

    private static byte[] image(String format) throws Exception {
        int type = "jpeg".equals(format) || "bmp".equals(format)
                ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage image = new BufferedImage(3, 2, type);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.BLUE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!ImageIO.write(image, format, output)) {
            throw new IllegalStateException("No ImageIO writer for " + format);
        }
        return output.toByteArray();
    }

    private void assertInvalid(MockMultipartFile file) {
        assertThrows(ImageUploadValidator.InvalidImageUploadException.class,
                () -> ImageUploadValidator.validate(file));
    }
}
