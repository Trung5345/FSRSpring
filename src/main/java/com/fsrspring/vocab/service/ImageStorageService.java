package com.fsrspring.vocab.service;

import com.fsrspring.vocab.config.EnrichmentProperties;
import com.fsrspring.vocab.dto.ImageCandidate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ImageStorageService {

    private static final int MAX_WIDTH = 960;
    private static final float JPEG_QUALITY = 0.82f;

    private final RestTemplate restTemplate;
    private final EnrichmentProperties properties;

    public StoredImage downloadConvertAndStore(String word, ImageCandidate candidate) {
        byte[] source = restTemplate.getForObject(candidate.getDownloadUrl(), byte[].class);
        if (source == null || source.length == 0) {
            throw new IllegalStateException("Image download returned empty content");
        }
        try {
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(source));
            if (original == null) {
                throw new IllegalStateException("Downloaded image format is unsupported");
            }
            BufferedImage resized = resizeToJpegCanvas(original);
            byte[] jpeg = toJpeg(resized);
            String filename = filename(word, candidate, jpeg);
            Path target = Path.of(properties.getEnrichment().getImageStorageDir()).toAbsolutePath().normalize().resolve(filename);
            Files.createDirectories(target.getParent());
            Files.write(target, jpeg);
            return new StoredImage(publicUrl(filename), jpeg.length);
        } catch (Exception e) {
            throw new IllegalStateException("Could not store image: " + e.getMessage(), e);
        }
    }

    private BufferedImage resizeToJpegCanvas(BufferedImage original) {
        int targetWidth = Math.min(MAX_WIDTH, original.getWidth());
        int targetHeight = Math.max(1, Math.round(original.getHeight() * (targetWidth / (float) original.getWidth())));
        Image scaled = original.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
        BufferedImage canvas = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = canvas.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, targetWidth, targetHeight);
        graphics.drawImage(scaled, 0, 0, null);
        graphics.dispose();
        return canvas;
    }

    private byte[] toJpeg(BufferedImage image) throws Exception {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new IllegalStateException("No JPEG writer available");
        }
        ImageWriter writer = writers.next();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(imageOutput);
            ImageWriteParam params = writer.getDefaultWriteParam();
            if (params.canWriteCompressed()) {
                params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                params.setCompressionQuality(JPEG_QUALITY);
            }
            writer.write(null, new IIOImage(image, null, null), params);
            return output.toByteArray();
        } finally {
            writer.dispose();
        }
    }

    private String filename(String word, ImageCandidate candidate, byte[] bytes) throws Exception {
        String prefix = slug(word) + "-" + candidate.getProvider().toLowerCase() + "-" + slug(candidate.getProviderImageId());
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String hash = HexFormat.of().formatHex(digest.digest(bytes)).substring(0, 12);
        return prefix + "-" + hash + ".jpg";
    }

    private String publicUrl(String filename) {
        String prefix = properties.getEnrichment().getImagePublicPrefix();
        if (prefix == null || prefix.isBlank()) {
            prefix = "/generated-images/";
        }
        if (!prefix.startsWith("/")) {
            prefix = "/" + prefix;
        }
        if (!prefix.endsWith("/")) {
            prefix = prefix + "/";
        }
        return prefix + filename;
    }

    public boolean canServe(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return false;
        }
        return localPathForPublicUrl(imageUrl)
                .map(Files::isRegularFile)
                .orElse(true);
    }

    private Optional<Path> localPathForPublicUrl(String imageUrl) {
        String requestPath = requestPath(imageUrl);
        String prefix = normalizePublicPrefix();
        if (!requestPath.startsWith(prefix)) {
            return Optional.empty();
        }

        String relative = URLDecoder.decode(requestPath.substring(prefix.length()), StandardCharsets.UTF_8);
        Path root = storageRoot();
        Path target = root.resolve(relative).normalize();
        if (relative.isBlank() || !target.startsWith(root)) {
            return Optional.of(root.resolve("__invalid_image_path__"));
        }
        return Optional.of(target);
    }

    private String requestPath(String imageUrl) {
        String value = imageUrl.trim();
        try {
            URI uri = URI.create(value);
            if (uri.getPath() != null && !uri.getPath().isBlank()) {
                return uri.getPath();
            }
        } catch (IllegalArgumentException ignored) {
            // Fall through and treat the value as a relative URL/path.
        }
        int queryIndex = value.indexOf('?');
        int fragmentIndex = value.indexOf('#');
        int end = value.length();
        if (queryIndex >= 0) {
            end = Math.min(end, queryIndex);
        }
        if (fragmentIndex >= 0) {
            end = Math.min(end, fragmentIndex);
        }
        return value.substring(0, end);
    }

    private Path storageRoot() {
        return Path.of(properties.getEnrichment().getImageStorageDir()).toAbsolutePath().normalize();
    }

    private String normalizePublicPrefix() {
        String prefix = properties.getEnrichment().getImagePublicPrefix();
        if (prefix == null || prefix.isBlank()) {
            prefix = "/generated-images/";
        }
        if (!prefix.startsWith("/")) {
            prefix = "/" + prefix;
        }
        if (!prefix.endsWith("/")) {
            prefix = prefix + "/";
        }
        return prefix;
    }

    private String slug(String value) {
        String cleaned = value == null ? "" : value.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-");
        cleaned = cleaned.replaceAll("^-|-$", "");
        return cleaned.isBlank() ? "image" : cleaned;
    }

    public record StoredImage(String publicUrl, long byteSize) {
    }
}
