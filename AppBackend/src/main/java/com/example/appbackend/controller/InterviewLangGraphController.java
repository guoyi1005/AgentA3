package com.example.appbackend.controller;

import com.example.appbackend.service.InterviewLangGraphService;
import com.example.appbackend.service.InterviewDeepSeekService;
import com.example.appbackend.util.InterviewAuthSupport;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
public class InterviewLangGraphController {

    private final InterviewLangGraphService langGraphService;
    private final InterviewDeepSeekService deepSeekService;

    public InterviewLangGraphController(InterviewLangGraphService langGraphService,
                                        InterviewDeepSeekService deepSeekService) {
        this.langGraphService = langGraphService;
        this.deepSeekService = deepSeekService;
    }

    @GetMapping("/api/interview/ai/status")
    public Map<String, Object> status() {
        return deepSeekService.status();
    }

    @PostMapping(value = "/api/langgraph/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return langGraphService.chat(body, InterviewAuthSupport.authorization(request));
    }
}
