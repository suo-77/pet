package com.itheima.controller;

import com.itheima.common.context.UserContext;
import com.itheima.common.result.Result;
import com.itheima.dto.LoginDTO;
import com.itheima.dto.RegisterDTO;
import com.itheima.dto.SendSmsCodeDTO;
import com.itheima.dto.SmsLoginDTO;
import com.itheima.service.AuthService;
import com.itheima.vo.LoginVO;
import com.itheima.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 账号接口：注册 / 登录 / 短信验证码 / 登出 / 个人信息
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 发送短信验证码（异步发送，立即返回） */
    @PostMapping("/sendSmsCode")
    public Result<Void> sendSmsCode(@Valid @RequestBody SendSmsCodeDTO dto) {
        authService.sendSmsCode(dto.getPhone());
        return Result.success();
    }

    /** 账号密码注册，成功自动登录 */
    @PostMapping("/register")
    public Result<LoginVO> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.success(authService.register(dto));
    }

    /** 账号密码登录 */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    /** 短信验证码登录（无账号自动注册） */
    @PostMapping("/loginBySms")
    public Result<LoginVO> loginBySms(@Valid @RequestBody SmsLoginDTO dto) {
        return Result.success(authService.loginBySms(dto));
    }

    /** 当前登录用户信息 */
    @GetMapping("/info")
    public Result<UserVO> info() {
        return Result.success(authService.info(UserContext.getUserIdRequired()));
    }

    /** 退出登录 */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String token) {
        authService.logout(token);
        return Result.success();
    }
}
