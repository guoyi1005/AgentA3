package com.example.appbackend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "interview_evaluation")
public class InterviewEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false, length = 64)
    private String conversationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "job_role", length = 100)
    private String jobRole;

    @Column(nullable = false)
    private Integer score = 0;

    @Column(name = "core_conclusion", nullable = false, columnDefinition = "TEXT")
    private String coreConclusion;

    @Column(columnDefinition = "TEXT")
    private String strengths;

    @Column(columnDefinition = "TEXT")
    private String weaknesses;

    @Column(columnDefinition = "TEXT")
    private String improvements;

    /** 五个评估维度的 JSON（专业知识/技术深度/表达能力/逻辑思维/岗位匹配度）。 */
    @Column(name = "dimensions_json", columnDefinition = "TEXT")
    private String dimensionsJson;

    /** 逐题分析的 JSON 数组，包含题目、学生真实回答、AI 点评与得分。 */
    @Column(name = "question_analysis_json", columnDefinition = "TEXT")
    private String questionAnalysisJson;

    /** 生成该报告的模型名称，便于排查。 */
    @Column(length = 64)
    private String model;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (score == null) score = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
