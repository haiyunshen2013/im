package com.wish.im.server.netty.handler;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wish.im.common.message.Message;
import com.wish.im.common.message.MsgStatus;
import com.wish.im.server.constant.AccountType;
import com.wish.im.server.mvc.account.entity.Account;
import com.wish.im.server.mvc.account.service.AccountService;
import com.wish.im.server.netty.client.ClientContainer;
import com.wish.im.server.netty.client.ClientInfo;
import com.wish.im.server.netty.message.IOfflineMessageContainer;
import io.netty.channel.*;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.util.AttributeKey;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Set;

import static com.wish.im.common.message.MsgType.*;

/**
 * 描述
 *
 * @author shy
 * @since 2021/7/22
 */
@Component
@Slf4j
@ChannelHandler.Sharable
@AllArgsConstructor
public class ServerHandler extends SimpleChannelInboundHandler<Message> {
    public static final AttributeKey<Object> CLIENT_ATTR = AttributeKey.newInstance("CLIENT");

    private IOfflineMessageContainer offlineMessageContainer;

    private final AccountService accountService;

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
        Message.Header header = msg.getHeader();
        String fromId = header.getFromId();
        if (StringUtils.isBlank(fromId)) {
            ctx.channel().disconnect();
        }
        if (header.getMsgType() == SHAKEHANDS) {
            boolean loginSuccess = processLogin(ctx, msg);
            if (loginSuccess) {
                doShakeHands(ctx, msg);
            } else {
                Message.Header noLoginHeader = createHeader(fromId, SEND, MsgStatus.NOT_LOGIN.getValue());
                Message noLongin = new Message(noLoginHeader, "登录失败".getBytes(StandardCharsets.UTF_8));
                noLongin.setOriginId(msg.getId());
                Channel channel = ctx.channel();
                channel.writeAndFlush(noLongin);
                channel.disconnect();
                return;
            }
        } else {
            if (validLogin(msg)) {
                if (header.getMsgType() == HEART) {
                    doHeart(ctx, msg);
                } else if (header.getMsgType() == SEND) {
                    doSend(ctx, msg);
                    return;
                } else if (header.getMsgType() == ACK) {
                    doAck(msg);
                } else if (header.getMsgType() == REQUEST) {
                    ClientInfo clientInfo = ((ClientInfo) ctx.channel().attr(CLIENT_ATTR).get());
                    Account account = clientInfo.getAccount();
                    if (account.getType() == AccountType.ADMIN) {
                        ctx.fireChannelRead(msg);
                    } else {
                        // 权限不足
                        Message.Header noPermission = new Message.Header();
                        noPermission.setFromId("server");
                        noPermission.setToId(fromId);
                        noPermission.setMsgType(RESPONSE);
                        noPermission.setStatus(MsgStatus.FORBIDDEN.getValue());
                        Message noPermissionMsg = new Message(noPermission, "登录失效".getBytes(StandardCharsets.UTF_8));
                        noPermissionMsg.setOriginId(msg.getId());
                        ctx.channel().writeAndFlush(noPermissionMsg);
                        return;
                    }
                }
            } else {
                processInvalidLogin(ctx, msg);
                return;
            }
        }
        // 刷新心跳时间
        ClientContainer.getById(fromId).setLastBeat(System.currentTimeMillis());
    }

    private void processInvalidLogin(ChannelHandlerContext ctx, Message msg) {
        Channel channel = ctx.channel();
        ClientInfo clientInfo = ClientContainer.getById(msg.getHeader().getFromId());
        Message.Header expireHeader = createHeader(clientInfo.getId(), SEND, MsgStatus.TOKEN_EXPIRE.getValue());
        Message expireToken = new Message(expireHeader, "登录失效".getBytes(StandardCharsets.UTF_8));
        ClientContainer.removeById(clientInfo.getId());
        channel.writeAndFlush(expireToken);
        ctx.channel().disconnect();
    }

    /**
     * 校验token有效性
     *
     * @param msg 消息
     * @return true：有效
     */
    private boolean validLogin(Message msg) {
        Message.Header header = msg.getHeader();
        String fromId = header.getFromId();
        ClientInfo clientInfo = ClientContainer.getById(fromId);
        if (clientInfo != null) {
            Account account = accountService.getById(clientInfo.getAccount().getId());
            clientInfo.setAccount(account);
            return account.getExpireTime().isAfter(LocalDateTime.now());
        }
        return false;
    }

    /**
     * 处理登录验证逻辑,1.客户端携带令牌认证，2.客户端成功登陆以后
     *
     * @param ctx ctx
     * @param msg msg
     */
    private boolean processLogin(ChannelHandlerContext ctx, Message msg) {
        Message.Header header = msg.getHeader();
        String fromId = header.getFromId();
        String tokenStr = new String(msg.getBody(), StandardCharsets.UTF_8);
        ClientInfo from = ClientContainer.getById(fromId);
        Account account;
        if (from != null) {
            account = from.getAccount();
            from.getChannel().disconnect();
        } else {
            QueryWrapper<Account> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().eq(Account::getName, fromId)
                    .eq(Account::getPwd, tokenStr)
                    .gt(Account::getExpireTime, LocalDateTime.now());
            account = accountService.getOne(queryWrapper);
        }
        if (account == null) {
            return false;
        }
        ClientInfo clientInfo = new ClientInfo();
        clientInfo.setAccount(account);
        clientInfo.setId(fromId);
        Channel channel = ctx.channel();
        clientInfo.setChannel(channel);
        ClientContainer.add(clientInfo);
        channel.attr(CLIENT_ATTR).set(clientInfo);
        return true;

    }


    /**
     * 处理发送消息
     *
     * @param msg msg
     */
    private void doSend(ChannelHandlerContext ctx, Message msg) {
        String fromId = msg.getHeader().getFromId();
        log.debug("接收到{}的消息，id = {}", fromId, msg.getId());
        Message.Header header = createHeader(fromId, ACK, 1);
        Message ack = new Message(header, null);
        ack.setOriginId(msg.getId());
        ack.getHeader().setStatus(MsgStatus.SERVER_ACK.getValue());
        //如果接受端在线
        ClientInfo to = ClientContainer.getById(msg.getHeader().getToId());
        if (to != null) {
            transferMsg(msg, to);
        } else {
            // 对方不在线，要么缓存消息，等待下次发送，要么告诉发送端本次发送失败
            if (msg.getHeader().isEnableCache()) {
                putOffLienMsg(msg);
            } else {
                ack.getHeader().setStatus(MsgStatus.FAIL.getValue());
            }
        }
        log.debug("服务端发送ACK给发送端");
        ctx.channel().writeAndFlush(ack);
    }

    /**
     * 处理确认消息
     *
     * @param msg msg
     */
    private void doAck(Message msg) {
        // 客户端收到消息后发送确认回执
        log.debug("接受端[{}]已确认", msg.getHeader().getFromId());
        //如果接受端在线
        ClientInfo to = ClientContainer.getById(msg.getHeader().getToId());
        if (to != null) {
            //转发给接受端
            transferMsg(msg, to);
        } else {
            //如果对方离线，缓存起来，等用户上线立马发送
            putOffLienMsg(msg);
        }
    }

    /**
     * 回应心跳信息
     *
     * @param msg 客户端心跳
     */
    private void doHeart(ChannelHandlerContext ctx, Message msg) {
        log.trace("接收到{}的 心跳 消息，id = {}", msg.getHeader().getFromId(), msg.getId());
        // 回写心跳
        Message.Header header = createHeader(msg.getHeader().getFromId(), HEART, 1);
        Message heart = new Message(header, null);
        heart.setOriginId(msg.getId());
        ctx.writeAndFlush(heart);
    }

    /**
     * 回应握手消息,发送离线时缓存的消息
     * 处理登录验证逻辑
     *
     * @param msg 客户端发送的消息
     */
    private void doShakeHands(ChannelHandlerContext ctx, Message msg) {
        String fromId = msg.getHeader().getFromId();
        log.debug("接收到{}的 握手 消息，id = {}", fromId, msg.getId());
        Message.Header header = createHeader(msg.getHeader().getFromId(), SHAKEHANDS, 0);
        // 回应握手消息
        Message shakeHands = new Message(header, null);
        shakeHands.setOriginId(msg.getId());
        ctx.channel().writeAndFlush(shakeHands);
        // 发送离线消息
        Set<Message> offlineMsgs = offlineMessageContainer.getOfflineMsgByToId(fromId);
        offlineMsgs.forEach(offlineMsg -> {
            ChannelFuture channelFuture = ctx.writeAndFlush(offlineMsg);
            channelFuture.addListener(future -> {
                if (future.isSuccess()) {
                    offlineMessageContainer.removeOfflineMsg(offlineMsg);
                }
            });
        });
    }

    /**
     * 转发消息
     *
     * @param msg 原始消息
     * @param to  客户端
     */
    private void transferMsg(Message msg, ClientInfo to) {
        offlineMessageContainer.transferMsg(msg, to);
    }

    @NotNull
    private Message.Header createHeader(String toId, int msgType, int status) {
        Message.Header header = new Message.Header();
        header.setFromId("server");
        header.setToId(toId);
        header.setMsgType(msgType);
        header.setStatus(status);
        return header;
    }

    /**
     * 连接活动状态
     *
     * @param ctx 上下文
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        log.debug("{}上线", channel.remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        Channel channel = ctx.channel();
        ClientInfo clientInfo = processLoginOut(ctx);
        log.debug("{}下线", clientInfo == null ? channel.remoteAddress() : clientInfo.getId());
    }

    /**
     * 处理登出逻辑，归还token，但不关闭通道
     *
     * @param ctx ctx
     */
    private ClientInfo processLoginOut(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        ClientInfo clientInfo = ((ClientInfo) channel.attr(CLIENT_ATTR).get());
        if (clientInfo != null) {
            channel.attr(CLIENT_ATTR).set(null);
            ClientContainer.removeById(clientInfo.getId());
        }
        return clientInfo;
    }

    /**
     * 发生异常时主动断开连接
     * 异常：1.客户端主动断开
     *
     * @param ctx   ctx
     * @param cause cause
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Channel channel = ctx.channel();
        ClientInfo clientInfo = processLoginOut(ctx);
        Object obj = clientInfo == null ? channel.remoteAddress() : clientInfo.getId();
        if (cause instanceof ReadTimeoutException) {
            log.error("{}连接发生长时间未收到消息即将关闭", obj, cause);
        } else {
            log.error("{}连接发生异常", obj, cause);
        }
        super.exceptionCaught(ctx, cause);
        ctx.close();
    }

    /**
     * 缓存离线消息
     *
     * @param pack 消息
     */
    private void putOffLienMsg(Message pack) {
        offlineMessageContainer.putOffLienMsg(pack);
    }
}
