package com.usts.ob.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 * 消息处理handler
 * TextWebSocketFrame的作用是要用于websocket专门处理文本的对象 frame是消息的载体
 */
public class ChatHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    //用于记录和管理
    private static ChannelGroup clients = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
//        ctx.channel();
        //当客户端连接服务端之后 获取客户端的channel 并且放到websocket中进行管理
        clients.add(ctx.channel());

    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
//        super.handlerRemoved(ctx);
        //当触发handlerRemoved channelGroup会自动移除客户端的channel
//        clients.remove(ctx.channel());
        System.out.println("客户端断开 channel对应的长ID：" + ctx.channel().id().asLongText());
        System.out.println("客户端断开 channel对应的短ID：" + ctx.channel().id().asShortText());

    }

    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        //获得客户端传输的数据
        String context = msg.text();
        System.out.println("Received Msg: " + context);

        for (Channel channel: clients) {

            channel.writeAndFlush(
                    new TextWebSocketFrame("[服务器接收到消息：]" + "\n 当前消息为：" + context));
//            channel.writeAndFlush(context);
        }
        //结果与for一致
//        clients.writeAndFlush(context);

    }
}
