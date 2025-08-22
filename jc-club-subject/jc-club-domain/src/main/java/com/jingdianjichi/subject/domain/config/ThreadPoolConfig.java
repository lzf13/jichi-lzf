package com.jingdianjichi.subject.domain.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池的config管理
 *
 * @author: ChickenWing
 * @date: 2023/11/26
 */
@Configuration
public class ThreadPoolConfig {

    /**
     * 创建并配置标签处理线程池
     *
     * @return 配置好的ThreadPoolExecutor实例，用于处理标签相关任务
     *
     * 线程池配置说明：
     * - 核心线程数：20
     * - 最大线程数：100
     * - 空闲线程存活时间：5秒
     * - 工作队列：容量为40的LinkedBlockingDeque
     * - 线程工厂：使用自定义名称的线程工厂，线程名前缀为"label"
     * - 拒绝策略：调用者运行策略，当线程池无法处理任务时由调用线程执行
     */
    @Bean(name = "labelThreadPool")
    public ThreadPoolExecutor getLabelThreadPool() {
        return new ThreadPoolExecutor(20, 100, 5,
                TimeUnit.SECONDS, new LinkedBlockingDeque<>(40),
                new CustomNameThreadFactory("label"),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

}
