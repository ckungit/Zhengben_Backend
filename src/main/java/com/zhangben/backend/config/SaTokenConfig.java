package com.zhangben.backend.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(new SaInterceptor(handler -> {

            // 获取当前请求方法（使用 Sa-Token 内置方法，兼容性更好）
            String method = SaHolder.getRequest().getMethod();
            
            // OPTIONS 预检请求直接放行（解决CORS问题）
            if ("OPTIONS".equalsIgnoreCase(method)) {
                return;
            }

            SaRouter.match("/**")
                    // 登录、注册不拦截
                    .notMatch("/api/auth/login", "/api/auth/register")

                    // 静态资源不拦截
                    .notMatch("/static/**", "/public/**", "/resources/**", "/favicon.ico")

                    // 其他全部需要登录（静默模式，不输出日志）
                    .check(r -> StpUtil.checkLogin());

        })).addPathPatterns("/**");
    }
}
