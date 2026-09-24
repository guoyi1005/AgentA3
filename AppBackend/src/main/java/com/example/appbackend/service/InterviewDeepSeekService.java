package com.example.appbackend.service;

import com.example.appbackend.exception.InterviewServiceException;
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
import java.util.Map;

@Service
public class InterviewDeepSeekService {

    private static final String DEFAULT_BASE_URL = "https://api.deepseek.com";
    private static final String DEFAULT_MODEL = "deepseek-chat";

    private final Environment environment;
    private final SystemConfigService systemConfigService;
    private final ObjectMapper objectMapper;

    public InterviewDeepSeekService(Environment environment,
                                    SystemConfigService systemConfigService,
                                    ObjectMapper objectMapper) {
        this.environment = environment;
        this.systemConfigService = systemConfigService;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("configured", StringUtils.hasText(resolveApiKey()));
        result.put("base_url", resolveBaseUrl());
        result.put("model", resolveModel());
        return result;
    }

    public String chat(List<Map<String, String>> messages) {
        String apiKey = resolveApiKey();
        if (!StringUtils.hasText(apiKey)) {
            throw new InterviewServiceException("interview_ai_not_configured", 503);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", resolveModel());
        payload.put("messages", messages);
        payload.put("temperature", 0.55);
        payload.put("max_tokens", 320);
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
                systemConfigService.getValue("ai.service.text.deepseek-chat.api-key", ""),
                isConfiguredDeepSeekDefault() ? systemConfigService.getValue("ai.service.text.api-key", "") : ""
        );
        return value == null ? "" : value;
    }

    private String resolveBaseUrl() {
        String configured = firstNonBlank(
                environment.getProperty("DEEPSEEK_BASE_URL"),
                environment.getProperty("interview.deepseek.base-url"),
                DEFAULT_BASE_URL
        );
        return configured.replaceAll("/+$", "");
    }

    private String resolveModel() {
        return firstNonBlank(
                environment.getProperty("DEEPSEEK_MODEL"),
                environment.getProperty("interview.deepseek.model"),
                DEFAULT_MODEL
        );
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
