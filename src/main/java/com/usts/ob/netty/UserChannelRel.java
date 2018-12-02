package com.usts.ob.netty;

import io.netty.channel.Channel;

import java.util.HashMap;

/**
 * @Description: 用户id和channel的关联关系处理
 */
public class UserChannelRel {

    private static HashMap<String, Channel> manager = new HashMap<>();

    /**
     * senderId channel相关联
     * @param senderId
     * @param channel
     */
    public static void put(String senderId, Channel channel) {
        manager.put(senderId, channel);
    }

    public static Channel get(String senderId) {
        return manager.get(senderId);
    }

    /**
     * 用于测试 打印userid和channel信息
     */
    public static void output() {
        for (HashMap.Entry<String, Channel> entry : manager.entrySet()) {
            System.out.println("UserId: " + entry.getKey()
                    + ", ChannelId: " + entry.getValue().id().asLongText());
        }
    }
}
