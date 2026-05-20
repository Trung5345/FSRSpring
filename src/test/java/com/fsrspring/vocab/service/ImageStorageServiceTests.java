package com.fsrspring.vocab.service;

import com.fsrspring.vocab.config.EnrichmentProperties;
import com.fsrspring.vocab.dto.ImageCandidate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImageStorageServiceTests {

    @TempDir
    Path tempDir;

    @Test
    void downloadsConvertsAndStoresJpegUnderPublicPrefix() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.getForObject("https://example.test/source.png", byte[].class)).thenReturn(sourcePng());

        EnrichmentProperties properties = new EnrichmentProperties();
        properties.getEnrichment().setImageStorageDir(tempDir.toString());
        properties.getEnrichment().setImagePublicPrefix("/generated-images/");

        ImageStorageService service = new ImageStorageService(restTemplate, properties);
        ImageStorageService.StoredImage stored = service.downloadConvertAndStore("meticulous", ImageCandidate.builder()
                .provider("PIXABAY")
                .providerImageId("123")
                .downloadUrl("https://example.test/source.png")
                .build());

        assertThat(stored.publicUrl()).startsWith("/generated-images/meticulous-pixabay-123-");
        assertThat(stored.publicUrl()).endsWith(".jpg");
        Path storedFile = Files.list(tempDir).findFirst().orElseThrow();
        assertThat(storedFile.getFileName().toString()).endsWith(".jpg");
        BufferedImage image = ImageIO.read(storedFile.toFile());
        assertThat(image.getWidth()).isLessThanOrEqualTo(960);
        assertThat(service.canServe(stored.publicUrl())).isTrue();
        assertThat(service.canServe("/generated-images/missing.jpg")).isFalse();
        assertThat(service.canServe("https://images.example.test/meticulous.jpg")).isTrue();
    }

    private byte[] sourcePng() throws Exception {
        BufferedImage image = new BufferedImage(1200, 800, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(0, 101, 144, 180));
        graphics.fillRect(0, 0, 1200, 800);
        graphics.dispose();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
