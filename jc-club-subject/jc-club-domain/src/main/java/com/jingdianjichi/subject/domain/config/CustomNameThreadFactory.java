package com.jingdianjichi.subject.domain.config;

import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 自定义名称的线程工厂
 *
 * @author: ChickenWing
 * @date: 2023/11/26
 */
public class CustomNameThreadFactory implements ThreadFactory {

    private static final AtomicInteger poolNumber = new AtomicInteger(1);
    private final ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;

    /**
     * 自定义线程名称的线程工厂构造函数
     * @param name 线程池名称前缀
     */
    CustomNameThreadFactory(String name) {
        // 获取系统安全管理器
        SecurityManager s = System.getSecurityManager();
        // 如果安全管理器存在，则使用其线程组，否则使用当前线程的线程组
        group = (s != null) ? s.getThreadGroup() :
                Thread.currentThread().getThreadGroup();
        // 如果传入的线程名称为空，则使用默认名称"pool"
        if (StringUtils.isBlank(name)) {
            name = "pool";
        }
        // 设置线程名称前缀，格式为：name-poolNumber-thread-
        namePrefix = name + "-" +
                poolNumber.getAndIncrement() + // 使用原子递增生成唯一序号
                "-thread-";
    }

    /**
     * 创建一个新线程
     * @param r 要执行的任务
     * @return 配置好的新线程
     */
    @Override
    public Thread newThread(Runnable r) {
        // 创建一个新线程，使用指定的线程组、任务、名称和堆栈大小
        Thread t = new Thread(group, r,
                namePrefix + threadNumber.getAndIncrement(), // 使用原子递增生成唯一序号
                0);
        // 确保线程不是守护线程
        if (t.isDaemon()){
            t.setDaemon(false);
        }
        // 确保线程优先级为正常优先级
        if (t.getPriority() != Thread.NORM_PRIORITY){
            t.setPriority(Thread.NORM_PRIORITY);
        }
        return t;
    }

}
