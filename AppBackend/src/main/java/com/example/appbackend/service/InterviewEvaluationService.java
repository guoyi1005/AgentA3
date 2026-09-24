package com.example.appbackend.service;

import com.example.appbackend.entity.AiInterviewMessage;
import com.example.appbackend.entity.InterviewConversation;
import com.example.appbackend.entity.InterviewEvaluation;
import com.example.appbackend.entity.InterviewUserProfile;
import com.example.appbackend.exception.InterviewServiceException;
import com.example.appbackend.prompt.InterviewPrompts;
import com.example.appbackend.repository.AiInterviewMessageRepository;
import com.example.appbackend.repository.InterviewConversationRepository;
import com.example.appbackend.repository.InterviewEvaluationRepository;
import com.example.appbackend.repository.InterviewUserProfileRepository;
import com.example.appbackend.util.InterviewJsonHelper;
import com.example.appbackend.util.InterviewMaps;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * AI 面试评估报告服务。
 *
 * 评估内容全部来自本次面试真实保存的问答记录（ai_interview_message），
 * 由 DeepSeek 生成结构化 JSON 后落库（interview_evaluation），
 * 页面再次打开时直接读取数据库结果，不会重复调用 AI。
 */
@Service
public class InterviewEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(InterviewEvaluationService.class);

    /** 单条回答送入模型的最大长度，避免超长上下文。 */
    private static final int MAX_ANSWER_CHARS = 1500;

    /** 评估报告 JSON 较长，必须给足输出预算，否则会被截断成非法 JSON。 */
    private static final int REPORT_MAX_TOKENS = 3000;

    private static final List<String> DIMENSION_KEYS = List.of(
            "professionalKnowledge", "technicalDepth", "expression", "logicalThinking", "jobMatching");

    private final InterviewEvaluationRepository evaluationRepository;
    private final InterviewConversationRepository conversationRepository;
    private final AiInterviewMessageRepository messageRepository;
    private final InterviewUserProfileRepository profileRepository;
    private final InterviewDeepSeekService deepSeekService;
    private final ObjectMapper objectMapper;

    public InterviewEvaluationService(InterviewEvaluationRepository evaluationRepository,
                                      InterviewConversationRepository conversationRepository,
                                      AiInterviewMessageRepository messageRepository,
                                      InterviewUserProfileRepository profileRepository,
                                      InterviewDeepSeekService deepSeekService,
                                      ObjectMapper objectMapper) {
        this.evaluationRepository = evaluationRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.profileRepository = profileRepository;
        this.deepSeekService = deepSeekService;
        this.objectMapper = objectMapper;
    }

    /**
     * 只读取已生成的报告，不调用 AI。用于页面刷新与结果轮询。
     */
    @Transactional(readOnly = true)
    public Map<String, Object> readReport(String conversationId) {
        if (!StringUtils.hasText(conversationId)) {
            return failure("MISSING_CONVERSATION_ID", "缺少会话ID，无法加载AI报告");
        }
        Optional<InterviewEvaluation> existing = evaluationRepository.findFirstByConversationIdOrderByIdDesc(conversationId);
        if (existing.isEmpty()) {
            InterviewConversation conversation = conversationRepository.findByConversationId(conversationId).orElse(null);
            if (conversation == null) {
                return failure("CONVERSATION_NOT_FOUND", "未找到本次面试记录");
            }
            Map<String, Object> body = failure("REPORT_NOT_GENERATED", "AI 正在生成面试评估报告……");
            body.put("conversation_id", conversationId);
            return body;
        }
        InterviewConversation conversation = conversationRepository.findByConversationId(conversationId).orElse(null);
        return success(existing.get(), conversation);
    }

    /**
     * 生成（或复用）本次面试的评估报告。已存在报告时直接返回，不重复调用 AI。
     */
    @Transactional
    public Map<String, Object> summarize(String conversationId) {
        if (!StringUtils.hasText(conversationId)) {
            return failure("MISSING_CONVERSATION_ID", "缺少会话ID，无法生成AI报告");
        }

        Optional<InterviewEvaluation> existing = evaluationRepository.findFirstByConversationIdOrderByIdDesc(conversationId);
        if (existing.isPresent()) {
            InterviewConversation conversation = conversationRepository.findByConversationId(conversationId).orElse(null);
            return success(existing.get(), conversation);
        }

        InterviewConversation conversation = conversationRepository.findByConversationId(conversationId).orElse(null);
        if (conversation == null) {
            return failure("CONVERSATION_NOT_FOUND", "未找到本次面试记录");
        }

        List<Map<String, String>> qaPairs = buildQaPairs(messageRepository.findByConversationIdOrderByIdAsc(conversationId));
        if (qaPairs.isEmpty()) {
            return failure("NO_INTERVIEW_CONTENT", "本次面试没有可用于评估的问答记录");
        }

        InterviewUserProfile profile = profileRepository.findByUserId(conversation.getUserId()).orElse(null);
        String input = buildEvaluationInput(conversation, profile, qaPairs);

        try {
            List<Map<String, String>> promptMessages = List.of(
                    Map.of("role", "system", "content", InterviewPrompts.EVALUATION_SYSTEM_PROMPT),
                    Map.of("role", "user", "content", input));
            // 评估报告是长结构化 JSON：单独放大输出预算并降低随机性。
            String raw = deepSeekService.chat(promptMessages, REPORT_MAX_TOKENS, 0.2);
            Map<String, Object> parsed = parseJsonObject(raw);
            if (parsed.isEmpty()) {
                log.error("AI 评估报告不是合法 JSON，conversationId={}，返回片段={}", conversationId, snippet(raw));
                return failure("AI_REPORT_GENERATION_FAILED", "AI 评估报告生成失败，请稍后重试");
            }

            InterviewEvaluation evaluation = new InterviewEvaluation();
            evaluation.setConversationId(conversationId);
            evaluation.setUserId(conversation.getUserId());
            evaluation.setJobRole(conversation.getJobRole());
            evaluation.setScore(toScore(parsed));
            evaluation.setCoreConclusion(orDefault(
                    firstText(parsed, "summary", "core_conclusion"), "暂无总体评价"));
            evaluation.setStrengths(String.join("\n", normalizeStringList(
                    firstValue(parsed, "strengths"), 5)));
            evaluation.setWeaknesses(String.join("\n", normalizeStringList(
                    firstValue(parsed, "weaknesses"), 5)));
            evaluation.setImprovements(String.join("\n", normalizeStringList(
                    firstValue(parsed, "suggestions", "improvements"), 5)));
            evaluation.setDimensionsJson(objectMapper.writeValueAsString(
                    normalizeDimensions(firstValue(parsed, "dimensions"))));
            evaluation.setQuestionAnalysisJson(objectMapper.writeValueAsString(
                    buildQuestionAnalysis(firstValue(parsed, "questionAnalysis", "question_analysis"), qaPairs)));
            evaluation.setModel(String.valueOf(deepSeekService.status().getOrDefault("model", "")));

            InterviewEvaluation saved = evaluationRepository.save(evaluation);
            updateProfileStats(profile);
            return success(saved, conversation);
        } catch (InterviewServiceException error) {
            log.error("AI 评估报告生成失败 conversationId={} code={} detail={}",
                    conversationId, error.getCode(), error.getMessage(), error);
            return failure(aiErrorCode(error.getCode()), aiErrorMessage(error.getCode()));
        } catch (Exception error) {
            log.error("AI 评估报告生成失败 conversationId={}", conversationId, error);
            return failure("AI_REPORT_GENERATION_FAILED", "AI 评估报告生成失败，请稍后重试");
        }
    }

    /** 按面试顺序把「面试官提问 + 候选人回答」配成问答对，只保留有真实回答的题目。 */
    private List<Map<String, String>> buildQaPairs(List<AiInterviewMessage> messages) {
        List<Map<String, String>> pairs = new ArrayList<>();
        String pendingQuestion = null;
        for (AiInterviewMessage message : messages) {
            String content = message.getContent() == null ? "" : message.getContent().trim();
            if (content.isEmpty()) {
                continue;
            }
            if ("assistant".equalsIgnoreCase(message.getRole())) {
                pendingQuestion = content;
            } else if ("user".equalsIgnoreCase(message.getRole()) && pendingQuestion != null) {
                pairs.add(qaPair(pendingQuestion, content));
                pendingQuestion = null;
            }
        }
        return pairs;
    }

    private Map<String, String> qaPair(String question, String answer) {
        Map<String, String> pair = new LinkedHashMap<>();
        pair.put("question", question);
        pair.put("answer", answer);
        return pair;
    }

    private String buildEvaluationInput(InterviewConversation conversation,
                                        InterviewUserProfile profile,
                                        List<Map<String, String>> qaPairs) {
        StringBuilder sb = new StringBuilder();
        sb.append("目标岗位：")
                .append(Optional.ofNullable(conversation.getJobRole()).orElse("通用"))
                .append('\n');
        if (profile != null) {
            sb.append("候选人背景：学历=").append(orDefault(profile.getEducation(), "未知"))
                    .append("，学校=").append(orDefault(profile.getSchool(), "未知"))
                    .append("，专业=").append(orDefault(profile.getMajor(), "未知"))
                    .append("，工作年限=").append(Optional.ofNullable(profile.getWorkExperienceYears()).orElse(0))
                    .append("，技能标签=").append(orDefault(profile.getSkillTags(), "无"))
                    .append('\n');
        }
        sb.append("本次面试共 ").append(qaPairs.size()).append(" 道已作答题目，问答记录如下：\n");
        for (int i = 0; i < qaPairs.size(); i++) {
            sb.append("第").append(i + 1).append("题：").append(qaPairs.get(i).get("question")).append('\n');
            sb.append("候选人回答：").append(trim(qaPairs.get(i).get("answer"), MAX_ANSWER_CHARS)).append('\n');
        }
        return sb.toString();
    }

    private Map<String, Object> success(InterviewEvaluation evaluation, InterviewConversation conversation) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("code", "OK");
        body.put("report", reportPayload(evaluation, conversation));
        return body;
    }

    private Map<String, Object> failure(String code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("code", code);
        body.put("message", message);
        return body;
    }

    private Map<String, Object> reportPayload(InterviewEvaluation evaluation, InterviewConversation conversation) {
        Map<String, Object> payload = new LinkedHashMap<>(InterviewMaps.evaluationItem(evaluation));
        payload.put("model", evaluation.getModel());
        payload.put("dimensions", readJsonObject(evaluation.getDimensionsJson()));
        payload.put("question_analysis", readJsonArray(evaluation.getQuestionAnalysisJson()));

        LocalDateTime startedAt = conversation == null ? null : conversation.getStartedAt();
        LocalDateTime endedAt = conversation == null ? null : conversation.getEndedAt();
        payload.put("started_at", startedAt == null ? null : startedAt.toString());
        payload.put("ended_at", endedAt == null ? null : endedAt.toString());
        Long durationSeconds = null;
        if (startedAt != null) {
            LocalDateTime end = endedAt != null ? endedAt : LocalDateTime.now();
            durationSeconds = Math.max(0L, Duration.between(startedAt, end).getSeconds());
        }
        payload.put("duration_seconds", durationSeconds);
        return payload;
    }

    /** 模型可能返回 ```json 包裹、前后附带说明文字，甚至夹带多余内容，这里做容错解析。 */
    private Map<String, Object> parseJsonObject(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.isEmpty()) {
            return Map.of();
        }
        text = text.replace("```json", "").replace("```JSON", "").replace("```", "").trim();
        Map<String, Object> direct = InterviewJsonHelper.extractJsonObject(text);
        if (!direct.isEmpty()) {
            return direct;
        }
        int start = text.indexOf('{');
        if (start < 0) {
            return Map.of();
        }
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return InterviewJsonHelper.extractJsonObject(text.substring(start, i + 1));
                }
            }
        }
        return Map.of();
    }

    private Map<String, Object> normalizeDimensions(Object raw) {
        Map<String, Object> source = asMap(raw);
        Map<String, Object> dimensions = new LinkedHashMap<>();
        for (String key : DIMENSION_KEYS) {
            Map<String, Object> item = new LinkedHashMap<>();
            Map<String, Object> value = asMap(source.get(key));
            item.put("score", clampScore(InterviewJsonHelper.asInt(value.get("score"), 0)));
            item.put("comment", InterviewJsonHelper.asText(value.get("comment"), ""));
            dimensions.put(key, item);
        }
        return dimensions;
    }

    private List<Map<String, Object>> buildQuestionAnalysis(Object raw, List<Map<String, String>> qaPairs) {
        List<Map<String, Object>> analysis = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (int i = 0; i < list.size(); i++) {
                Map<String, Object> source = asMap(list.get(i));
                if (source.isEmpty()) {
                    continue;
                }
                String question = InterviewJsonHelper.asText(source.get("question"), "");
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("question", question.isEmpty() && i < qaPairs.size()
                        ? qaPairs.get(i).get("question") : question);
                item.put("answer", matchAnswer(question, i, qaPairs));
                item.put("answer_summary", InterviewJsonHelper.asText(
                        firstValue(source, "answerSummary", "answer_summary"), ""));
                item.put("score", clampScore(InterviewJsonHelper.asInt(source.get("score"), 0)));
                item.put("comment", InterviewJsonHelper.asText(source.get("comment"), ""));
                analysis.add(item);
            }
        }
        if (!analysis.isEmpty()) {
            return analysis;
        }
        // 模型未给出逐题分析时，至少回填本次真实问答，避免页面空白。
        for (Map<String, String> pair : qaPairs) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("question", pair.get("question"));
            item.put("answer", pair.get("answer"));
            item.put("answer_summary", "");
            item.put("score", 0);
            item.put("comment", "");
            analysis.add(item);
        }
        return analysis;
    }

    private String matchAnswer(String question, int index, List<Map<String, String>> qaPairs) {
        if (StringUtils.hasText(question)) {
            for (Map<String, String> pair : qaPairs) {
                String candidate = pair.get("question");
                if (candidate != null && (candidate.contains(question) || question.contains(candidate))) {
                    return pair.get("answer");
                }
            }
            for (Map<String, String> pair : qaPairs) {
                if (shareKeyword(question, pair.get("question"))) {
                    return pair.get("answer");
                }
            }
        }
        return index < qaPairs.size() ? qaPairs.get(index).get("answer") : "";
    }

    private boolean shareKeyword(String left, String right) {
        if (!StringUtils.hasText(left) || !StringUtils.hasText(right)) {
            return false;
        }
        String a = left.replaceAll("[\\s，。？?！!、,.;:：\"'（）()]", "");
        String b = right.replaceAll("[\\s，。？?！!、,.;:：\"'（）()]", "");
        int window = Math.min(6, Math.min(a.length(), b.length()));
        if (window <= 0) {
            return false;
        }
        for (int i = 0; i + window <= a.length(); i++) {
            if (b.contains(a.substring(i, i + window))) {
                return true;
            }
        }
        return false;
    }

    private int toScore(Map<String, Object> parsed) {
        Object value = firstValue(parsed, "overallScore", "overall_score", "score");
        return clampScore(InterviewJsonHelper.asInt(value, 0));
    }

    private List<String> normalizeStringList(Object raw, int max) {
        List<String> items = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                String text = item == null ? "" : String.valueOf(item).trim();
                if (!text.isEmpty()) {
                    items.add(text);
                }
            }
        } else if (raw != null) {
            String text = String.valueOf(raw).trim();
            if (!text.isEmpty()) {
                items.addAll(Arrays.stream(text.split("[\\n;；。]"))
                        .map(String::trim)
                        .filter(part -> !part.isEmpty())
                        .toList());
            }
        }
        return items.size() > max ? new ArrayList<>(items.subList(0, max)) : items;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object raw) {
        if (raw instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private Map<String, Object> readJsonObject(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception error) {
            log.warn("解析报告维度 JSON 失败", error);
            return Map.of();
        }
    }

    private List<Map<String, Object>> readJsonArray(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception error) {
            log.warn("解析报告逐题分析 JSON 失败", error);
            return List.of();
        }
    }

    private Object firstValue(Map<String, Object> source, String... keys) {
        for (String key : keys) {
            Object value = source.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String firstText(Map<String, Object> source, String... keys) {
        return InterviewJsonHelper.asText(firstValue(source, keys), "");
    }

    private String aiErrorCode(String code) {
        if ("interview_ai_not_configured".equals(code)) {
            return "AI_NOT_CONFIGURED";
        }
        return "AI_REPORT_GENERATION_FAILED";
    }

    private String aiErrorMessage(String code) {
        if ("interview_ai_not_configured".equals(code)) {
            return "AI 评估服务尚未配置，暂时无法生成评估报告";
        }
        return "AI 评估报告生成失败，请稍后重试";
    }

    private void updateProfileStats(InterviewUserProfile profile) {
        if (profile == null) {
            return;
        }
        try {
            profile.setAiReportCount(Optional.ofNullable(profile.getAiReportCount()).orElse(0) + 1);
            List<InterviewEvaluation> all = evaluationRepository.findByUserIdOrderByIdDesc(profile.getUserId());
            double average = all.stream()
                    .mapToInt(item -> Optional.ofNullable(item.getScore()).orElse(0))
                    .average()
                    .orElse(0);
            profile.setAverageScore(BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP));
            profileRepository.save(profile);
        } catch (Exception error) {
            log.warn("更新候选人报告统计失败 userId={}", profile.getUserId(), error);
        }
    }

    private int clampScore(int score) {
        if (score < 0) {
            return 0;
        }
        return Math.min(score, 100);
    }

    private String trim(String value, int max) {
        String text = value == null ? "" : value.trim();
        return text.length() <= max ? text : text.substring(0, max) + "……";
    }

    private String snippet(String value) {
        return trim(value, 200);
    }

    private String orDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}
