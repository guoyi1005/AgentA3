package com.example.appbackend.service;

import com.example.appbackend.entity.AiInterviewMessage;
import com.example.appbackend.entity.InterviewConfig;
import com.example.appbackend.entity.InterviewConversation;
import com.example.appbackend.entity.InterviewUserProfile;
import com.example.appbackend.exception.InterviewServiceException;
import com.example.appbackend.prompt.InterviewPrompts;
import com.example.appbackend.repository.AiInterviewMessageRepository;
import com.example.appbackend.repository.InterviewConfigRepository;
import com.example.appbackend.repository.InterviewConversationRepository;
import com.example.appbackend.repository.InterviewUserProfileRepository;
import com.example.appbackend.util.InterviewJsonHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class InterviewLangGraphService {

    private final InterviewConversationRepository conversationRepository;
    private final AiInterviewMessageRepository messageRepository;
    private final InterviewConfigRepository configRepository;
    private final InterviewUserProfileRepository profileRepository;
    private final InterviewDeepSeekService deepSeekService;
    private final ObjectMapper objectMapper;

    public InterviewLangGraphService(InterviewConversationRepository conversationRepository,
                                     AiInterviewMessageRepository messageRepository,
                                     InterviewConfigRepository configRepository,
                                     InterviewUserProfileRepository profileRepository,
                                     InterviewDeepSeekService deepSeekService,
                                     ObjectMapper objectMapper) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.configRepository = configRepository;
        this.profileRepository = profileRepository;
        this.deepSeekService = deepSeekService;
        this.objectMapper = objectMapper;
    }

    public SseEmitter chat(Map<String, Object> body, String authorization) {
        String conversationId = InterviewJsonHelper.asStr(body.get("conversation_id"));
        String content = InterviewJsonHelper.asStr(body.get("content"));
        boolean initialQuestion = Boolean.TRUE.equals(body.get("initial_question"))
                || "true".equalsIgnoreCase(InterviewJsonHelper.asStr(body.get("initial_question")));
        if (!StringUtils.hasText(conversationId)) {
            throw new InterviewServiceException("missing_or_empty_field", 400, Map.of("field", "conversation_id"));
        }
        if (!initialQuestion && !StringUtils.hasText(content)) {
            throw new InterviewServiceException("missing_or_empty_field", 400, Map.of("field", "content"));
        }
        InterviewConversation conversation = conversationRepository.findByConversationId(conversationId)
                .orElseThrow(() -> new InterviewServiceException("conversation_not_found", 400));

        if (!initialQuestion) {
            persistMessage(conversation, "user", content, null);
        }

        String jobRole = Optional.ofNullable(InterviewJsonHelper.asStr(body.get("job_role")))
                .filter(StringUtils::hasText)
                .orElse(Optional.ofNullable(conversation.getJobRole()).orElse("通用"));

        InterviewConfig cfg = configRepository.findFirstByConversationIdOrderByIdDesc(conversationId).orElse(null);
        String system = InterviewPrompts.systemText(jobRole);
        if (cfg != null) {
            String extra = InterviewPrompts.buildConfigContext(
                    cfg.getInterviewMode(), cfg.getInterviewerPersona(), cfg.getFocusTags(),
                    cfg.getRequirements(), cfg.getDifficulty());
            if (StringUtils.hasText(extra)) {
                system = system + "\n" + extra;
            }
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> userInfo = body.get("user_info") instanceof Map<?, ?> ui
                ? (Map<String, Object>) ui : null;
        if (userInfo == null) {
            InterviewUserProfile profile = profileRepository.findByUserId(conversation.getUserId()).orElse(null);
            if (profile != null) {
                userInfo = new LinkedHashMap<>();
                userInfo.put("nickname", profile.getNickname());
                userInfo.put("education", profile.getEducation());
                userInfo.put("school", profile.getSchool());
                userInfo.put("major", profile.getMajor());
                userInfo.put("work_experience_years", profile.getWorkExperienceYears());
                userInfo.put("skill_tags", profile.getSkillTags());
                userInfo.put("target_position", profile.getTargetPosition());
            }
        }

        if (userInfo != null) {
            system = system + "\n候选人信息：" + userInfo;
        }

        List<Map<String, String>> promptMessages = new ArrayList<>();
        promptMessages.add(Map.of("role", "system", "content", system));
        List<AiInterviewMessage> history = messageRepository.findByConversationIdOrderByIdAsc(conversationId);
        for (AiInterviewMessage message : history) {
            String role = "assistant".equalsIgnoreCase(message.getRole()) ? "assistant" : "user";
            promptMessages.add(Map.of("role", role, "content", message.getContent()));
        }
        if (initialQuestion && history.isEmpty()) {
            promptMessages.add(Map.of(
                    "role", "user",
                    "content", "请开始这场模拟面试。先简短问候，然后只提出一道与“" + jobRole + "”岗位相关的第一题。"
            ));
        }

        SseEmitter emitter = new SseEmitter(0L);
        CompletableFuture.runAsync(() -> {
            try {
                String answer = deepSeekService.chat(promptMessages);
                emitContentChunks(emitter, answer);
                persistMessage(conversation, "assistant", answer, null);
                emitter.send(SseEmitter.event().data("[DONE]"));
                emitter.complete();
            } catch (Exception e) {
                try {
                    String code = e instanceof InterviewServiceException serviceError
                            ? serviceError.getCode() : "interview_ai_request_failed";
                    Map<String, Object> err = Map.of("error", code);
                    emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(err)));
                    emitter.send(SseEmitter.event().data("[DONE]"));
                } catch (Exception ignored) {
                }
                emitter.complete();
            }
        });
        return emitter;
    }

    private void emitContentChunks(SseEmitter emitter, String answer) throws IOException {
        int size = 24;
        for (int i = 0; i < answer.length(); i += size) {
            String chunk = answer.substring(i, Math.min(i + size, answer.length()));
            Map<String, Object> payload = Map.of("content", chunk);
            emitter.send(SseEmitter.event()
                    .data(objectMapper.writeValueAsString(payload), MediaType.APPLICATION_JSON));
        }
    }

    @Transactional
    protected void persistMessage(InterviewConversation conversation, String role, String content, String emotion) {
        AiInterviewMessage msg = new AiInterviewMessage();
        msg.setConversationPk(conversation.getId());
        msg.setConversationId(conversation.getConversationId());
        msg.setRole(role);
        msg.setContent(content);
        msg.setEmotionLabel(emotion);
        messageRepository.save(msg);
    }
}
