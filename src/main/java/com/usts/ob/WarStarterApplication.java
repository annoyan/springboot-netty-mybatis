package com.usts.ob;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * 使用外置application启动
 */

@SpringBootApplication
@ServletComponentScan

public class WarStarterApplication extends SpringBootServletInitializer {


    public WarStarterApplication() {
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(MchatApplication.class);
    }
}
