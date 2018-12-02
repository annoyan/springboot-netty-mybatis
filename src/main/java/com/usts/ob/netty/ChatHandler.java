package com.usts.ob.netty;

import com.usts.ob.SpringUtil;
import com.usts.ob.enums.MsgActionEnum;
import com.usts.ob.service.UserService;
import com.usts.ob.utils.JsonUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 消息处理handler
 * TextWebSocketFrame的作用是要用于websocket专门处理文本的对象 frame是消息的载体
 *
 * 将users增加到channel中 通过websocket统一进行管理
 */
public class ChatHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    //用于记录和管理
    public static ChannelGroup users = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);


    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
//        ctx.channel();
        //当客户端连接服务端之后 获取客户端的channel 并且放到websocket中进行管理
        users.add(ctx.channel());

    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        //当触发handlerRemoved channelGroup会自动移除客户端的channel
        String channelId = ctx.channel().id().asShortText();
        System.out.println("客户端被移除，channelId为：" + channelId);
        /**
         * 一旦客户端发生异常 应当先关闭当前users 在进行关闭
         * 并且exceptionCaught()中移除客户端
          */
        users.remove(ctx.channel());

    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {


        /**
         * 1 获取客户端发来的消息
         * 2 判断消息类型 处理不同的业务
         * 2.1 当websocket 第一次open的时候，初始化channel，把用的channel和userid关联起来
         * 2.2 聊天类型的消息，把聊天记录保存到数据库，同时标记消息的签收状态[未签收]----只保存原始数据 不加密
         * 2.3 签收消息类型，针对具体的消息进行签收，修改数据库中对应消息的签收状态[已签收]
         * 2.4 心跳类型的消息
         */

        /**
         * 1 获取客户端发来的消息
         *   为消息创建实体类datacontent
         */
        String content = msg.text();
        Channel currentChannel = ctx.channel();

        DataContent dataContent = JsonUtils.jsonToPojo(content, DataContent.class);
        Integer action = dataContent.getAction();

        if (action == MsgActionEnum.CONNECT.type) {

            /**
             * 传入操作类型
             * 2.1 当websocket 第一次open的时候，初始化channel，把用的channel和userid关联起来
             */
            String senderId = dataContent.getChatMsg().getSenderId();

            UserChannelRel.put(senderId, currentChannel);

            /**
             * Test 打印所有的用户channel
             */
            for (Channel c :users) {
                System.out.println(c.id().asLongText());
            }

            UserChannelRel.output();
        } else if (action == MsgActionEnum.CHAT.type) {

            /**
             * 2.2 聊天类型的消息，
             * 把聊天记录保存到数据库，--不能通过netty直接将msg内容保存到数据库--通过springutil类保存
             * 同时标记消息的签收状态[未签收]----只保存原始数据 不加密
             */
            ChatMsg chatMsg = dataContent.getChatMsg();
            String msgText = chatMsg.getMsg();
            String receiverId = chatMsg.getReceiverId();
            String senderId = chatMsg.getSenderId();

            /**
             * 使用springUtil获取userserice对象 将msg的内容保存到数据库中
             */
            UserService userService = (UserService) SpringUtil.getBean("userServiceImpl");
            String msgId = userService.saveMsg(chatMsg);
            /**
             * 丢出msgid给接受者
             */
            chatMsg.setMsgId(msgId);

            DataContent dataContentMsg = new DataContent();
            dataContentMsg.setChatMsg(chatMsg);

            /**
             * 发送消息-- 从全局用户channel的关系中获取接收方的channel
             */

            Channel receiverChannel = UserChannelRel.get(receiverId);
            if (receiverChannel == null) {
                /**
                 *
                 * TODO channel为空代表用户离线 推送消息 Jpush个推 小米推送
                 */

            } else {
                /**
                 * channel不为空  则要到channelgroup中查找对应的channel是否存在
                 */
                Channel findChannel = users.find(receiverChannel.id());
                if (findChannel != null) {
                    /**
                     *  用户在线 消息发送
                     */

                    receiverChannel.writeAndFlush(
                            new TextWebSocketFrame(
                                    JsonUtils.objectToJson(dataContentMsg)));
                } else {
                    /**
                     * 用户离线 消息推送
                     */
                }
            }

        } else if (action == MsgActionEnum. SIGNED.type) {

            /**
             * 2.3 签收消息类型，针对具体的消息进行签收，修改数据库中对应消息的签收状态[已签收]
             * 不是用户以查阅 而是发送到接收方手机即为签收
             */

            UserService userService = (UserService)SpringUtil.getBean("userServiceImpl");
            /**
             * 扩展字段在signed类型的消息中 代表需要去签收的消息id
             * 逗号间隔
             * 前端发送的消息 都是以逗号间隔 msgid通过逗号相间隔
             */
            String msgIdsStr = dataContent.getExtand();
            String msgIds[] = msgIdsStr.split(",");
            List<String> msgIdList = new ArrayList<>();

            for (String mid : msgIds) {
//                判断每一项是否为空
                if (StringUtils.isNotBlank(mid)) {
                    msgIdList.add(mid);
                }
            }

            System.out.println(msgIdList.toString());

            if (msgIdList != null && !msgIdList.isEmpty() && msgIdList.size() > 0) {
                /**
                 * 批量签收
                 */
                userService.updateMsgSigned(msgIdList);
            }

        } else if (action == MsgActionEnum.KEEPALIVE.type) {

            /**
             * TODO 2.4 心跳类型的消息
             */

            System.out.println("收到来自channel为[" + currentChannel + "]的心跳包");
        }
//        System.out.println("Received Msg: " + content);
//
//        for (Channel channel: users) {
//
//            /**
//             *    返回给前端
//             *    可能发生错误
//             *    [ERROR] : SyntaxError: Unexpected EOFfile name:chatlist.htmlline no:1
//             */
//
//            channel.writeAndFlush(
//                    new TextWebSocketFrame("[服务器接收到消息：]" + " 当前消息为：" + null));
////            channel.writeAndFlush(context);
//        }
//        //结果与for一致
////        users.writeAndFlush(context);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

        cause.printStackTrace();
        /**
         * 异常之后 关闭channel 随后从channelgroup中移除
         */
        ctx.channel().close();
        users.remove(ctx.channel());
    }
}
