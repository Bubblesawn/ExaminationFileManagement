package com.exam.record;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @brief 考籍管理系统后端启动类。
 *
 * @details
 * 负责启动 Spring Boot 应用，并加载系统管理、考籍档案、业务审核、
 * 智能辅助等后端模块配置。
 */
@SpringBootApplication
public class ExaminationRecordApplication {

    /**
     * @brief 应用程序入口方法。
     *
     * @param args 命令行启动参数。
     */
    public static void main(String[] args) {
        SpringApplication.run(ExaminationRecordApplication.class, args);
    }
}

