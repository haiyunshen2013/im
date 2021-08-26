package com.wish.im.server.netty.handler;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wish.im.common.message.Message;
import com.wish.im.common.message.MsgStatus;
import com.wish.im.server.constant.AccountType;
import com.wish.im.server.mvc.account.entity.Account;
import com.wish.im.server.mvc.account.service.AccountService;
import com.wish.im.server.mvc.message.entity.MessageLog;
import com.wish.im.server.mvc.message.service.MessageLogService;
import com.wish.im.server.netty.client.ClientContainer;
import com.wish.im.server.netty.client.ClientInfo;
import io.netty.channel.*;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.util.AttributeKey;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

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

    private MessageLogService messageLogService;

    private final AccountService accountService;

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
        String fromId = msg.getFromId();
        if (StringUtils.isBlank(fromId)) {
            ctx.channel().disconnect();
        }
        if (msg.getType() == SHAKEHANDS) {
            boolean loginSuccess = processLogin(ctx, msg);
            if (loginSuccess) {
                doShakeHands(ctx, msg);
            } else {
                Message noLongin = Message.builder().fromId("server")
                        .toId(fromId).type(SEND).status(MsgStatus.NOT_LOGIN.getValue())
                        .body("登录失败".getBytes(StandardCharsets.UTF_8)).build();
                noLongin.setOriginId(msg.getId());
                Channel channel = ctx.channel();
                channel.writeAndFlush(noLongin);
                channel.disconnect();
                return;
            }
        } else {
            if (validLogin(msg)) {
                if (msg.getType() == HEART) {
                    doHeart(ctx, msg);
                } else if (msg.getType() == SEND) {
                    doSend(ctx, msg);
                    return;
                } else if (msg.getType() == ACK) {
                    doAck(ctx, msg);
                } else if (msg.getType() == REQUEST) {
                    ClientInfo clientInfo = ((ClientInfo) ctx.channel().attr(CLIENT_ATTR).get());
                    Account account = clientInfo.getAccount();
                    if (account.getType() == AccountType.ADMIN) {
                        ctx.fireChannelRead(msg);
                    } else {
                        // 权限不足
                        Message noPermissionMsg = Message.builder().fromId("server")
                                .toId(fromId).type(RESPONSE).status(MsgStatus.FORBIDDEN.getValue())
                                .body("权限不足".getBytes(StandardCharsets.UTF_8)).build();
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
        ClientInfo clientInfo = ClientContainer.getById(fromId);
        if (clientInfo != null) {
            clientInfo.setLastBeat(System.currentTimeMillis());
        }
    }

    private void processInvalidLogin(ChannelHandlerContext ctx, Message msg) {
        Channel channel = ctx.channel();
        ClientInfo clientInfo = ClientContainer.getById(msg.getFromId());
        if (clientInfo == null) {
            ctx.channel().disconnect();
            return;
        }
        Message expireToken = Message.builder().fromId("server").toId(clientInfo.getId()).type(SEND).status(MsgStatus.TOKEN_EXPIRE.getValue())
                .body("登录失效".getBytes(StandardCharsets.UTF_8)).build();
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
        String fromId = msg.getFromId();
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
        String fromId = msg.getFromId();
        String tokenStr = new String(msg.getBody(), StandardCharsets.UTF_8);
        MessageLog messageLog = convertMessageLog(msg, ctx.channel());
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
            messageLog.setStatus(MsgStatus.NOT_LOGIN.getValue());
            messageLogService.save(messageLog);
            return false;
        }
        ClientInfo clientInfo = new ClientInfo();
        clientInfo.setAccount(account);
        clientInfo.setId(fromId);
        Channel channel = ctx.channel();
        clientInfo.setChannel(channel);
        ClientContainer.add(clientInfo);
        channel.attr(CLIENT_ATTR).set(clientInfo);
        messageLog.setStatus(MsgStatus.OK.getValue());
        messageLog.setSuccess(true);
        messageLogService.save(messageLog);
        return true;
    }


    /**
     * 处理发送消息
     *
     * @param msg msg
     */
    private void doSend(ChannelHandlerContext ctx, Message msg) {
        String fromId = msg.getFromId();
        log.debug("接收到{}的消息，id = {}", fromId, msg.getId());
        Message ack = Message.builder().fromId("server").toId(fromId)
                .originId(msg.getId()).type(ACK)
                .status(MsgStatus.SERVER_ACK.getValue()).build();
        ack.setStatus(MsgStatus.SERVER_ACK.getValue());
        //如果接受端在线
        String toId = msg.getToId();
        if (StringUtils.isBlank(toId)) {
            return;
        }
        MessageLog messageLog = convertMessageLog(msg, ctx.channel());
        ClientInfo to = ClientContainer.getById(toId);
        if (to != null) {
            messageLog.setToChannelId(to.getChannel().id().asLongText());
            transferMsg(msg, to);
            messageLog.setSuccess(true);
        } else {
            // 对方不在线，要么缓存消息，等待下次发送，要么告诉发送端本次发送失败
            if (!msg.isEnableCache()) {
                messageLog.setStatus(MsgStatus.FAIL.getValue());
                ack.setStatus(MsgStatus.FAIL.getValue());
            }
        }
        messageLogService.save(messageLog);
        log.debug("服务端发送ACK给发送端");
        ctx.channel().writeAndFlush(ack);
    }

    private MessageLog convertMessageLog(Message msg, Channel fromChannel) {
        MessageLog messageLog = new MessageLog();
        BeanUtils.copyProperties(msg, messageLog);
        messageLog.setRemoteAddress(fromChannel.remoteAddress().toString());
        messageLog.setFromChannelId(fromChannel.id().asLongText());
        return messageLog;
    }

    /**
     * 处理确认消息
     *
     * @param msg msg
     */
    private void doAck(ChannelHandlerContext ctx, Message msg) {
        // 客户端收到消息后发送确认回执
        log.debug("接受端[{}]已确认", msg.getFromId());
        //如果接受端在线
        ClientInfo to = ClientContainer.getById(msg.getToId());
        MessageLog messageLog = convertMessageLog(msg, ctx.channel());
        if (to != null) {
            //转发给接受端
            transferMsg(msg, to);
            messageLog.setToChannelId(to.getChannel().id().asLongText());
            messageLog.setSuccess(true);
        }
        messageLogService.save(messageLog);
    }

    /**
     * 回应心跳信息
     *
     * @param msg 客户端心跳
     */
    private void doHeart(ChannelHandlerContext ctx, Message msg) {
        log.trace("接收到{}的 心跳 消息，id = {}", msg.getFromId(), msg.getId());
        // 回写心跳
        Message heart = Message.builder().fromId("server").toId(msg.getFromId()).type(HEART)
                .status(MsgStatus.SERVER_ACK.getValue()).originId(msg.getId()).build();
        ctx.writeAndFlush(heart);
    }

    /**
     * 回应握手消息,发送离线时缓存的消息
     * 处理登录验证逻辑
     *
     * @param msg 客户端发送的消息
     */
    private void doShakeHands(ChannelHandlerContext ctx, Message msg) {
        String fromId = msg.getFromId();
        log.debug("接收到{}的 握手 消息，id = {}", fromId, msg.getId());
        // 回应握手消息
        Message shakeHands = Message.builder().fromId("server").toId(fromId).type(SHAKEHANDS).status(MsgStatus.SERVER_ACK.getValue())
                .originId(msg.getId()).build();
        ctx.channel().writeAndFlush(shakeHands);
        // 发送离线消息
        List<MessageLog> list = messageLogService.lambdaQuery().eq(MessageLog::getToId, fromId)
                .eq(MessageLog::isEnableCache, true).eq(MessageLog::isSuccess, false).list();
        list.forEach(offlineMsg -> {
            ChannelFuture channelFuture = ctx.writeAndFlush(offlineMsg);
            channelFuture.addListener(future -> {
                if (future.isSuccess()) {
                    offlineMsg.setSuccess(true);
                    messageLogService.updateById(offlineMsg);
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
        //转发给接受端
        ChannelFuture channelFuture = to.getChannel().writeAndFlush(msg);
        channelFuture.addListener((ChannelFutureListener) future -> {
            MessageLog messageLog = messageLogService.getById(msg.getId());
            if (messageLog == null) {
                return;
            }
            if (future.isSuccess() && !messageLog.isSuccess()) {
                messageLog.setToChannelId(to.getChannel().id().asLongText());
                messageLog.setSuccess(true);
                messageLogService.updateById(messageLog);
            }
        });
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
}
