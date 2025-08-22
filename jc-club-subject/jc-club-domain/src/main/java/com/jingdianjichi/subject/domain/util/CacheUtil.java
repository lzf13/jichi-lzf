package com.jingdianjichi.subject.domain.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jingdianjichi.subject.domain.entity.SubjectCategoryBO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 缓存工具类
 *
 * @author: ChickenWing
 * @date: 2023/12/3
 */
@Component
public class CacheUtil<K, V> {

    private Cache<String, String> localCache =
            CacheBuilder.newBuilder()
                    .maximumSize(5000)
                    .expireAfterWrite(10, TimeUnit.SECONDS)
                    .build();


    public List<V> getResult(String cacheKey, Class<V> clazz,
                             Function<String, List<V>> function) {
        List<V> resultList = new ArrayList<>();
        String content = localCache.getIfPresent(cacheKey);
        if (StringUtils.isNotBlank(content)) {
            resultList = JSON.parseArray(content, clazz);
        } else {
            resultList = function.apply(cacheKey);
            if (!CollectionUtils.isEmpty(resultList)) {
                localCache.put(cacheKey, JSON.toJSONString(resultList));
            }
        }
        return resultList;
    }

    /**
     * 缓存 Map 结果,lzf新添加
     */
    public Map<K, V> getMapResult(String cacheKey, Class<V> clazz,
                                  Function<String, Map<K, V>> function) {
        // 1) 先查缓存
        String content = localCache.getIfPresent(cacheKey);
        if (StringUtils.isNotBlank(content)) {
            // 命中：JSON -> Map<K,V>
            JSONObject obj = JSON.parseObject(content);
            Map<K, V> result = new HashMap<>(obj.size());
            obj.forEach((k, v) -> {
                V value = (v == null) ? null : JSON.parseObject(JSON.toJSONString(v), clazz);
                result.put(castKey(k), value);
            });
            return result;
        }

        // 2) 未命中：加载并写回缓存
        Map<K, V> loaded = function.apply(cacheKey);
        if (!CollectionUtils.isEmpty(loaded)) {
            localCache.put(cacheKey, JSON.toJSONString(loaded));
            return loaded;
        }
        return new HashMap<>();
    }

/**
 * 缓存 key 转换为 K,lzf新添加
 */
    @SuppressWarnings("unchecked")
    private K castKey(String keyStr) {
        if (keyStr == null) {
            return null;
        }
        // 纯数字（可带负号），转成 Long；否则保留为 String
        boolean isNumeric = keyStr.chars().allMatch(ch -> ch == '-' || Character.isDigit(ch));
        if (isNumeric && !(keyStr.length() == 1 && keyStr.charAt(0) == '-')) {
            try {
                return (K) Long.valueOf(keyStr);
            } catch (NumberFormatException ignore) {
                // 超出 Long 范围或解析失败时回退为 String
            }
        }
        return (K) keyStr;
    }

}
