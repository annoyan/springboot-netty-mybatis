package com.usts.ob.netty;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

public class WSServerInitialzer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        // websocket基于http协议 定义http解码器
        pipeline.addLast(new HttpServerCodec());
        //对写大数据流的支持
        pipeline.addLast(new ChunkedWriteHandler());
//        定义消息的最大长度 将httpMessage聚合成FullHttpRequest and FullHttpResponse
        pipeline.addLast(new HttpObjectAggregator(1024*64));
/**
 * 以上定义支持http协议 用于指定给客户端
 */

/**
 * websocket服务器处理的协议 用于指定给客户端连接访问的路由"/ws"
 * 此handler会处理handshaking （including close ping and pong）
 * 对于websocket来讲 都是以frames传输 不同类型的数据其frames也不同
 *
 */
        pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));

        /**
         * 用户自定的handler
         */
        pipeline.addLast(new ChatHandler());
    }
}
