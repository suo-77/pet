package com.itheima.interceptor;

import com.itheima.common.constant.RedisKeyConstant;
import com.itheima.common.context.UserContext;
import com.itheima.common.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * 登录拦截器：校验请求头 Authorization 中的 token
 * 1. JWT 签名/有效期校验 + Redis 登录态校验（退出登录/被顶号立即失效）
 * 2. 匿名白名单：登录与非登录均放行，且尽力解析 token 注入 UserContext，
 *    支撑"未登录可见、登录后增强"的接口（信息流点赞状态、商品详情收藏状态、关注列表默认查自己等）
 * 3. 非白名单路径无有效 token 一律 401
 * 校验通过后把 userId 写入 UserContext，请求结束清理
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /** 匿名白名单：放行未登录访问，并尽力注入登录态 */
    private static final List<String> ANON_WHITELIST = List.of(
            "/api/post/page",            // 社区信息流
            "/api/post/*/comments",      // 帖子评论列表
            "/api/post/*/likes",         // 帖子点赞用户列表（谁赞了）
            "/api/user/home/*",          // 用户主页
            "/api/user/following",       // 关注列表（匿名需传 userId）
            "/api/user/fans",            // 粉丝列表（匿名需传 userId）
            "/api/goods/*",              // 商品详情（登录返回收藏状态）
            "/api/goods/*/evaluates",    // 商品评价列表
            "/api/goods/shop/*",         // 店铺商品列表
            "/api/shop/*",               // 店铺详情（/my、/open 例外，见下）
            "/api/shop/*/coupons"        // 店铺优惠券列表
    );

    /** 白名单中必须登录的例外路径（店主管理接口，不能匿名） */
    private static final List<String> WHITELIST_LOGIN_REQUIRED = List.of(
            "/api/shop/my",
            "/api/shop/open"
    );

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        Long userId = resolveUserId(request.getHeader("Authorization"));
        if (userId != null) {
            // 有效登录态：无论是否白名单，均注入上下文供业务使用
            UserContext.setUserId(userId);
            return true;
        }
        // 无有效登录态：白名单匿名放行（店主管理接口除外），受保护接口 401
        if (isAnonWhitelist(request.getRequestURI())
                && !WHITELIST_LOGIN_REQUIRED.contains(request.getRequestURI())) {
            return true;
        }
        writeUnauthorized(response);
        return false;
    }

    /** 解析 token -> userId（JWT + Redis 双校验），无效返回 null */
    private Long resolveUserId(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        Long userId;
        try {
            userId = jwtUtil.parseToken(token);
        } catch (Exception e) {
            return null;
        }
        // Redis 登录态二次校验：退出登录或 token 被删除立即失效
        String cachedUserId = stringRedisTemplate.opsForValue().get(RedisKeyConstant.LOGIN_TOKEN + token);
        if (cachedUserId == null || !cachedUserId.equals(String.valueOf(userId))) {
            return null;
        }
        return userId;
    }

    /** 请求路径是否命中匿名白名单 */
    private boolean isAnonWhitelist(String uri) {
        return ANON_WHITELIST.stream().anyMatch(pattern -> pathMatcher.match(pattern, uri));
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) {
        // 防止线程复用导致的用户串号
        UserContext.clear();
    }

    /** token 无效统一返回 401 + JSON */
    private void writeUnauthorized(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"msg\":\"未登录或登录已过期\",\"data\":null}");
    }
}
