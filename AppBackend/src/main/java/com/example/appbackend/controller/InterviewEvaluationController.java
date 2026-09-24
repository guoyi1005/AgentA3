package com.example.appbackend.controller;

import com.example.appbackend.service.InterviewEvaluationService;
import com.example.appbackend.util.InterviewJsonHelper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * AI 面试评估报告接口。
 *
 * 返回体统一为 {@code {success, code, message?, report?}}，
 * AI 调用失败、会话不存在等情况都返回可识别的错误码，不再直接抛 HTTP 500。
 */
@RestController
@RequestMapping("/api/interview/evaluation")
public class InterviewEvaluationController {

    private final InterviewEvaluationService evaluationService;

    public InterviewEvaluationController(InterviewEvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    /** 只读取数据库中已生成的报告，不调用 AI。 */
    @GetMapping("/report")
    public Map<String, Object> report(
            @RequestParam(value = "conversation_id", required = false) String conversationId) {
        return evaluationService.readReport(conversationId);
    }

    /** 生成报告；若已生成过则直接返回数据库结果，不会重复调用 AI。 */
    @PostMapping("/summarize")
    public Map<String, Object> summarize(@RequestBody(required = false) Map<String, Object> body) {
        String conversationId = body == null ? null : InterviewJsonHelper.asStr(body.get("conversation_id"));
        return evaluationService.summarize(conversationId);
    }
}
