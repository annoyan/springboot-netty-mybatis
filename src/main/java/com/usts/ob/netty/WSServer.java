package com.usts.ob.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.springframework.stereotype.Component;

/**
 * wsserver作为组件 让springboot启动
 * springboot启动完成 wsserver启动
 * 只能单例实现
 */

@Component
public class WSServer {
    //    保证单例获取
    private static class SingletionWSS {
        static final WSServer instance = new WSServer();
    }

    public static WSServer getWSServer() {
        return SingletionWSS.instance;
    }

    private EventLoopGroup mainGroup;
    private EventLoopGroup supGroup;
    private ServerBootstrap serverBootstrap;
    private ChannelFuture future;

    public WSServer() {
        mainGroup = new NioEventLoopGroup();
        supGroup = new NioEventLoopGroup();
        serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(mainGroup, supGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new WSServerInitialzer());
    }


    public void start() {
        this.future = serverBootstrap.bind(8088);
        System.err.println("netty websocket server started....");
    }
}

