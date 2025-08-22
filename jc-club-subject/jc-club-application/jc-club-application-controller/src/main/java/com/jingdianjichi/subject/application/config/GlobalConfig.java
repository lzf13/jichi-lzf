package com.jingdianjichi.subject.application.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jingdianjichi.subject.application.interceptor.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import java.util.List;

/**
 * mvc的全局处理
 *
 * @author: ChickenWing
 * @date: 2023/10/7
 */
@Configuration
public class GlobalConfig extends WebMvcConfigurationSupport {

    @Override
    protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        super.configureMessageConverters(converters);
        converters.add(mappingJackson2HttpMessageConverter());
    }

    /**
     * 添加拦截器方法
     * 重写WebMvcConfigurer中的addInterceptors方法，用于注册自定义拦截器
     * @param registry 拦截器注册器，用于注册拦截器
     */
    @Override
    protected void addInterceptors(InterceptorRegistry registry) {
        // 创建登录拦截器实例并添加到注册器中
        registry.addInterceptor(new LoginInterceptor())
                // 设置拦截器拦截所有路径（/**表示拦截所有请求路径）
                .addPathPatterns("/**");
    }

    /**
     * 自定义mappingJackson2HttpMessageConverter
     * 目前实现：空值忽略，空字段可返回
     */
    private MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return new MappingJackson2HttpMessageConverter(objectMapper);
    }


}
