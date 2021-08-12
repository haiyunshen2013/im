package com.wish.im.server.netty.client;

import com.wish.im.server.mvc.account.entity.Account;
import io.netty.channel.Channel;
import lombok.Data;


/**
 * 表示一个连接信息
 *
 * @author shy
 * @date 2021/7/26
 */
@Data
public class ClientInfo {
    /**
     * 实例的id，唯一，也可以是设备id
     */
    private String id;

    /**
     * 客户端名字
     */
    private String name;

    /**
     * 保存与客户端相关的通道信息
     */
    private Channel channel;

    /**
     * 健康状态 0 代表健康，数值越大，越不健康
     */
    private int health;

    /**
     * 心跳时间
     */
    private volatile long lastBeat = System.currentTimeMillis();

    private Account account;
}
