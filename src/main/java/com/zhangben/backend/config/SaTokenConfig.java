package com.zhangben.backend.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @SuppressWarnings("unused")
	@Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(new SaInterceptor(handler -> {

            SaRouter.match("/**")
                    // 登录、注册不拦截
                    .notMatch("/api/auth/login", "/api/auth/register")

                    // 静态资源不拦截
                    .notMatch("/static/**", "/public/**", "/resources/**", "/favicon.ico")

                    // 其他全部需要登录
                    .check(r -> StpUtil.checkLogin());

        })).addPathPatterns("/**");
    }
}