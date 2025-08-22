package com.jingdianjichi.subject.common.context;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录上下文对象
 *
 * @author: ChickenWing
 * @date: 2023/11/26
 */
public class LoginContextHolder {

    private static final InheritableThreadLocal<Map<String, Object>> THREAD_LOCAL
            = new InheritableThreadLocal<>();

    public static void set(String key, Object val) {
        Map<String, Object> map = getThreadLocalMap();
        map.put(key, val);
    }

    /**
     * 根据键获取线程本地存储中的值
     * @param key 要获取的值的键
     * @return 返回与键关联的值，如果不存在则返回null
     */
    public static Object get(String key){
        // 获取当前线程的线程本地存储Map
        Map<String, Object> threadLocalMap = getThreadLocalMap();
        // 从Map中获取并返回指定键的值
        return threadLocalMap.get(key);
    }

    /**
     * 获取当前线程的登录ID
     * @return 返回存储在ThreadLocal中的登录ID字符串
     */
    public static String getLoginId(){
        // 从ThreadLocalMap中获取名为"loginId"的值并返回
        return (String) getThreadLocalMap().get("loginId");
    }

    public static void remove(){
        THREAD_LOCAL.remove();
    }

    public static Map<String, Object> getThreadLocalMap() {
        Map<String, Object> map = THREAD_LOCAL.get();
        if (Objects.isNull(map)) {
            map = new ConcurrentHashMap<>();
            THREAD_LOCAL.set(map);
        }
        return map;
    }


}
