package com.syxs.module.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syxs.config.FileStorageConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OllamaClient {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient okHttpClient = new OkHttpClient();
    private final ObjectMapper objectMapper;
    private final FileStorageConfig fileStorageConfig;

    @Value("${syxs.ai.ollama.enabled:false}")
    private boolean ollamaEnabled;

    @Value("${syxs.ai.ollama.base-url:http://127.0.0.1:11434}")
    private String ollamaBaseUrl;

    @Value("${syxs.ai.ollama.model:llava:7b}")
    private String ollamaModel;

    public OllamaClient(ObjectMapper objectMapper, FileStorageConfig fileStorageConfig) {
        this.objectMapper = objectMapper;
        this.fileStorageConfig = fileStorageConfig;
    }

    public String estimateSummary(String title,
                                  String category,
                                  String brand,
                                  String condition,
                                  int yearsUsed,
                                  int rarity,
                                  String description,
                                  List<String> imageUrls) {
        List<String> imagePayloads = resolveImagePayloads(imageUrls);
        if (ollamaEnabled) {
            try {
                String content = requestOllamaSummary(
                    title,
                    category,
                    brand,
                    condition,
                    yearsUsed,
                    rarity,
                    description,
                    imagePayloads
                );
                if (content != null && !content.isBlank()) {
                    return content;
                }
            } catch (Exception ignored) {
            }
        }
        return buildFallbackSummary(title, category, brand, condition, yearsUsed, rarity, description, imagePayloads.size());
    }

    private String requestOllamaSummary(String title,
                                        String category,
                                        String brand,
                                        String condition,
                                        int yearsUsed,
                                        int rarity,
                                        String description,
                                        List<String> imagePayloads) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", ollamaModel);
        payload.put("stream", false);
        payload.put("prompt", buildPrompt(title, category, brand, condition, yearsUsed, rarity, description, imagePayloads.size()));
        if (!imagePayloads.isEmpty()) {
            payload.put("images", imagePayloads);
        }

        Request request = new Request.Builder()
            .url(stripTrailingSlash(ollamaBaseUrl) + "/api/generate")
            .post(RequestBody.create(objectMapper.writeValueAsString(payload), JSON_MEDIA_TYPE))
            .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Ollama request failed");
            }
            JsonNode jsonNode = objectMapper.readTree(response.body().string());
            return sanitize(jsonNode.path("response").asText(""));
        }
    }

    private List<String> resolveImagePayloads(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return List.of();
        }
        return imageUrls.stream()
            .limit(3)
            .map(this::resolveImagePath)
            .filter(path -> path != null && Files.exists(path))
            .map(this::encodeBase64)
            .filter(encoded -> encoded != null && !encoded.isBlank())
            .toList();
    }

    private Path resolveImagePath(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }
        if (!imageUrl.startsWith("/files/") && !imageUrl.startsWith("/uploads/")) {
            return null;
        }
        String filename = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
        if (filename.isBlank()) {
            return null;
        }
        Path root = Path.of(fileStorageConfig.getUploadDir()).toAbsolutePath().normalize();
        Path resolved = root.resolve(filename).normalize();
        if (!resolved.startsWith(root)) {
            return null;
        }
        return resolved;
    }

    private String encodeBase64(Path path) {
        try {
            return Base64.getEncoder().encodeToString(Files.readAllBytes(path));
        } catch (IOException ignored) {
            return null;
        }
    }

    private String buildPrompt(String title,
                               String category,
                               String brand,
                               String condition,
                               int yearsUsed,
                               int rarity,
                               String description,
                               int imageCount) {
        return """
            你是二手高价值商品估价助手。请基于以下信息，用中文输出一句估价摘要，60字以内，强调品牌、成色、流通性和建议。
            标题：%s
            分类：%s
            品牌：%s
            成色：%s
            使用年限：%d
            稀缺度：%d/5
            图片数量：%d
            描述：%s
            """.formatted(
            safeText(title),
            safeText(category),
            safeText(brand),
            safeText(condition),
            yearsUsed,
            rarity,
            imageCount,
            safeText(description)
        );
    }

    private String buildFallbackSummary(String title,
                                        String category,
                                        String brand,
                                        String condition,
                                        int yearsUsed,
                                        int rarity,
                                        String description,
                                        int imageCount) {
        StringBuilder builder = new StringBuilder();
        builder.append(safeText(title)).append(" 属于 ").append(safeText(category));
        if (!safeText(brand).isBlank()) {
            builder.append("，品牌为 ").append(brand.trim());
        }
        if (!safeText(condition).isBlank()) {
            builder.append("，当前成色 ").append(condition.trim());
        }
        builder.append("，已参考 ").append(imageCount).append(" 张图片、")
            .append(yearsUsed).append(" 年使用周期和 ")
            .append(rarity).append("/5 稀缺度");
        if (description != null && !description.isBlank()) {
            builder.append("，建议结合附件与描述细节定价");
        } else {
            builder.append("，补充附件与描述后可进一步提高准确性");
        }
        return builder.toString();
    }

    private String sanitize(String content) {
        if (content == null) {
            return "";
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        if (normalized.length() > 80) {
            return normalized.substring(0, 80);
        }
        return normalized;
    }

    private String stripTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "http://127.0.0.1:11434";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }
}
