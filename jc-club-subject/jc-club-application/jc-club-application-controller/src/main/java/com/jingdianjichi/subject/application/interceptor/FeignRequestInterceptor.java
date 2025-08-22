package com.jingdianjichi.subject.application.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Objects;

/**
 * Feign请求拦截器
 *
 * @author: ChickenWing
 * @date: 2023/12/3
 */
@Component
public class FeignRequestInterceptor implements RequestInterceptor {

    /**
     * 应用请求模板，将当前请求的loginId头信息传递到新的请求中
     * @param requestTemplate 请求模板对象，用于构建新的请求
     */
    @Override
    public void apply(RequestTemplate requestTemplate) {
        // 获取当前请求的属性对象
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        // 从请求属性中获取HttpServletRequest对象
        HttpServletRequest request = requestAttributes.getRequest();
        // 检查请求对象是否为空
        if (Objects.nonNull(request)) {
            // 从请求头中获取loginId信息
            String loginId = request.getHeader("loginId");
            // 检查loginId是否为空或空白字符串
            if (StringUtils.isNotBlank(loginId)) {
                // 将loginId添加到新的请求头中
                requestTemplate.header("loginId", loginId);
            }
        }
    }

}
