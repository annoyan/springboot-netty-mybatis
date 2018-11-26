package com.usts.ob;

import com.usts.ob.netty.WSServer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * 监听springboot
 */
@Component
public class NettyBooter implements ApplicationListener<ContextRefreshedEvent> {

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {

        //判断当前事件为空 启动netty
        if (contextRefreshedEvent.getApplicationContext().getParent() == null) {
            try {
                WSServer.getWSServer().start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
