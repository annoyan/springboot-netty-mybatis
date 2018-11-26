package com.usts.ob;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import tk.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
//扫描mybatis mapper包路径
@MapperScan(basePackages = {"com.usts.ob.mapper"})

@ComponentScan(basePackages = {"com.usts.ob", "org.n3r.idworker"})

public class MchatApplication {

    public static void main(String[] args) {
        SpringApplication.run(MchatApplication.class, args);
    }
}
