package com.example.appbackend.config;

import com.example.appbackend.repository.InterviewKnowledgeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * 复用项目已有的 AI 面试题库脚本。仅在题库为空时初始化，避免覆盖管理端维护的数据。
 */
@Component
public class InterviewQuestionBankSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InterviewQuestionBankSeeder.class);

    private final InterviewKnowledgeRepository knowledgeRepository;
    private final DataSource dataSource;

    public InterviewQuestionBankSeeder(InterviewKnowledgeRepository knowledgeRepository, DataSource dataSource) {
        this.knowledgeRepository = knowledgeRepository;
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (knowledgeRepository.count() > 0) {
            return;
        }
        ClassPathResource seed = new ClassPathResource("db/interview-seed.sql");
        if (!seed.exists()) {
            log.warn("未找到 AI 面试题库数据 db/interview-seed.sql，保持真实空题库");
            return;
        }
        try {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator(seed);
            populator.setSqlScriptEncoding("UTF-8");
            populator.execute(dataSource);
            log.info("AI 面试题库初始化完成，共 {} 道题", knowledgeRepository.count());
        } catch (Exception error) {
            log.error("AI 面试题库初始化失败，页面将显示可重试的空状态", error);
        }
    }
}
