package com.xxs.game;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

/**
 * 小学随堂知识闯关游戏 - 后端入口
 */
@SpringBootApplication
@MapperScan("com.xxs.game.mapper")
public class XxsGameApplication {

    public static void main(String[] args) {
        // 统一按北京时间计算「今天」，与数据库（服务器时区 CST）保持一致
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
        SpringApplication.run(XxsGameApplication.class, args);
    }
}