package com.itheima;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 项目启动类：宠物购买社交社区全栈系统
 * 包扫描根路径 com.itheima，MyBatis Mapper 接口统一扫描 com.itheima.mapper
 */
@SpringBootApplication
@MapperScan("com.itheima.mapper")
public class WebSpringboot01Application {

    public static void main(String[] args) {
        SpringApplication.run(WebSpringboot01Application.class, args);
    }

}
