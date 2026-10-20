package com.itheima.config;

import com.itheima.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置：
 * 1. 本地图片目录映射：/images/** 直接访问本地磁盘 D:/MYsuo/study/images 下的图片
 * 2. 跨域放行前端开发端口 8085（生产可收紧）
 * 3. Token 登录拦截器：默认全部拦截，公开接口在白名单中放行
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /** 图片本地存储目录（yml 中 pet.upload.dir） */
    @Value("${pet.upload.dir}")
    private String uploadDir;

    private final LoginInterceptor loginInterceptor;

    public WebMvcConfig(LoginInterceptor loginInterceptor) {
        this.loginInterceptor = loginInterceptor;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 访问 /images/xxx.jpg 时映射到本地磁盘文件
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("http://localhost:8085", "http://127.0.0.1:8085")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        // 账号模块公开接口（无登录态诉求）
                        "/api/auth/sendSmsCode",
                        "/api/auth/register",
                        "/api/auth/login",
                        "/api/auth/loginBySms",
                        // 店铺分页浏览与首页搜索（无登录态诉求，彻底跳过拦截器）
                        // 店铺详情/券列表走拦截器匿名白名单；/shop/my、/shop/open 必须登录（店主管理）
                        "/api/shop/page",
                        "/api/search",
                        // WebSocket 端点：自身在握手拦截器做 token 鉴权，不走 MVC 拦截器
                        "/ws/**",
                        // 静态图片资源与错误页
                        "/images/**",
                        "/error"
                );
        // 说明：信息流/评论/用户主页/关注粉丝/商品浏览等"匿名可访问"路径
        // 由 LoginInterceptor 内部白名单放行（尽力解析 token 注入 UserContext），
        // 保证登录用户访问这些接口也能拿到个性化数据（点赞状态/收藏状态/默认查自己）
    }
}
