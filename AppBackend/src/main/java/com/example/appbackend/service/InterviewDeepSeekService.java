package com.example.appbackend.service;

import com.example.appbackend.entity.AiModelConfig;
import com.example.appbackend.exception.InterviewServiceException;
import com.example.appbackend.repository.AiModelConfigRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class InterviewDeepSeekService {

    private static final String DEFAULT_BASE_URL = "https://api.deepseek.com";
    private static final String DEFAULT_MODEL = "deepseek-chat";
    private static final int DEFAULT_MAX_TOKENS = 320;
    private static final double DEFAULT_TEMPERATURE = 0.55;

    private final Environment environment;
    private final SystemConfigService systemConfigService;
    private final AiModelConfigRepository aiModelConfigRepository;
    private final ObjectMapper objectMapper;

    public InterviewDeepSeekService(Environment environment,
                                    SystemConfigService systemConfigService,
                                    AiModelConfigRepository aiModelConfigRepository,
                                    ObjectMapper objectMapper) {
        this.environment = environment;
        this.systemConfigService = systemConfigService;
        this.aiModelConfigRepository = aiModelConfigRepository;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("configured", StringUtils.hasText(resolveApiKey()));
        result.put("source", resolveApiKeySource());
        result.put("base_url", resolveBaseUrl());
        result.put("model", resolveModel());
        return result;
    }

    public String chat(List<Map<String, String>> messages) {
        return chat(messages, DEFAULT_MAX_TOKENS, DEFAULT_TEMPERATURE);
    }

    /**
     * 生成面试问题等短文本时使用默认预算；
     * 生成评估报告这类长结构化结果时必须显式放大 maxTokens，否则 JSON 会被截断。
     */
    public String chat(List<Map<String, String>> messages, int maxTokens, double temperature) {
        String apiKey = resolveApiKey();
        if (!StringUtils.hasText(apiKey)) {
            throw new InterviewServiceException("interview_ai_not_configured", 503);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", resolveModel());
        payload.put("messages", messages);
        payload.put("temperature", temperature);
        payload.put("max_tokens", maxTokens);
        payload.put("stream", false);

        try {
            String raw = WebClient.builder()
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build()
                    .post()
                    .uri(resolveChatCompletionsUrl())
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(40))
                    .block();

            JsonNode root = objectMapper.readTree(raw);
            String content = root.path("choices").path(0).path("message").path("content").asText("").trim();
            if (!StringUtils.hasText(content)) {
                throw new InterviewServiceException("interview_ai_empty_response", 502);
            }
            return content;
        } catch (WebClientResponseException error) {
            int status = error.getStatusCode().value();
            if (status == 401 || status == 403) {
                throw new InterviewServiceException("interview_ai_unauthorized", 502);
            }
            if (status == 429) {
                throw new InterviewServiceException("interview_ai_rate_limited", 503);
            }
            throw new InterviewServiceException("interview_ai_upstream_error", 502,
                    Map.of("upstream_status", status));
        } catch (InterviewServiceException error) {
            throw error;
        } catch (Exception error) {
            throw new InterviewServiceException("interview_ai_request_failed", 502);
        }
    }

    private String resolveApiKey() {
        String value = firstNonBlank(
                environment.getProperty("DEEPSEEK_API_KEY"),
                environment.getProperty("interview.deepseek.api-key"),
                adminConfiguredDeepSeek().map(AiModelConfig::getApiKey).orElse(""),
                systemConfigService.getValue("ai.service.text.deepseek-chat.api-key", ""),
                isConfiguredDeepSeekDefault() ? systemConfigService.getValue("ai.service.text.api-key", "") : ""
        );
        return value == null ? "" : value;
    }

    /**
     * 说明密钥来自哪里，便于部署排查；只返回来源名称，不返回任何密钥片段。
     */
    private String resolveApiKeySource() {
        if (StringUtils.hasText(environment.getProperty("DEEPSEEK_API_KEY"))
                || StringUtils.hasText(environment.getProperty("interview.deepseek.api-key"))) {
            return "env";
        }
        if (adminConfiguredDeepSeek().isPresent()) {
            return "database";
        }
        if (StringUtils.hasText(systemConfigService.getValue("ai.service.text.deepseek-chat.api-key", ""))) {
            return "system_config";
        }
        return StringUtils.hasText(resolveApiKey()) ? "system_config" : "none";
    }

    private String resolveBaseUrl() {
        String configured = firstNonBlank(
                environment.getProperty("DEEPSEEK_BASE_URL"),
                environment.getProperty("interview.deepseek.base-url"),
                adminConfiguredDeepSeek().map(AiModelConfig::getBaseUrl).orElse(""),
                DEFAULT_BASE_URL
        );
        return configured.replaceAll("/+$", "");
    }

    private String resolveModel() {
        return firstNonBlank(
                environment.getProperty("DEEPSEEK_MODEL"),
                environment.getProperty("interview.deepseek.model"),
                adminConfiguredDeepSeek().map(AiModelConfig::getModelName).orElse(""),
                DEFAULT_MODEL
        );
    }

    /**
     * 管理端配置的模型（ai_model_config.api_key 由 EncryptedStringConverter 加密落库）。
     * 只有形如 DeepSeek 的配置才会被面试官复用，避免把面试内容发给无关的服务商。
     */
    private Optional<AiModelConfig> adminConfiguredDeepSeek() {
        return aiModelConfigRepository.findAll().stream()
                .filter(config -> config.getStatus() != null && config.getStatus() == 1)
                .filter(this::isDeepSeekConfig)
                .filter(config -> StringUtils.hasText(config.getApiKey()))
                .findFirst();
    }

    private boolean isDeepSeekConfig(AiModelConfig config) {
        String haystack = String.join(" ",
                String.valueOf(config.getProvider()),
                String.valueOf(config.getBaseUrl()),
                String.valueOf(config.getModelName())).toLowerCase(Locale.ROOT);
        return haystack.contains("deepseek");
    }

    private String resolveChatCompletionsUrl() {
        return resolveBaseUrl() + "/chat/completions";
    }

    private boolean isConfiguredDeepSeekDefault() {
        String provider = systemConfigService.getValue("ai.service.text.provider", "");
        String model = systemConfigService.getValue("ai.service.text.model", "");
        return (provider + " " + model).toLowerCase().contains("deepseek");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }
}
