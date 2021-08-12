package com.wish.im.server.netty.message;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wish.im.common.message.Message;
import com.wish.im.common.message.MsgStatus;
import com.wish.im.common.message.MsgType;
import com.wish.im.server.mvc.offlinemessage.entity.OfflineMessage;
import com.wish.im.server.mvc.offlinemessage.service.OfflineMessageService;
import com.wish.im.server.netty.client.ClientContainer;
import com.wish.im.server.netty.client.ClientInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 描述
 *
 * @author shy
 * @since 2021/8/11
 */
@AllArgsConstructor
@Slf4j
public class DbOfflineMessageContainer implements IOfflineMessageContainer {
    private final OfflineMessageService offlineMessageService;

    @Override
    public void putOffLienMsg(Message pack) {
        OfflineMessage offlineMessage = new OfflineMessage();
        offlineMessage.setMessage(pack);
        Message.Header header = pack.getHeader();
        offlineMessage.setToId(header.getToId());
        offlineMessage.setMsgId(pack.getId());
        offlineMessage.insertAdapter(false, header.getFromId());
        if (containsMsg(pack)) {
            return;
        }
        offlineMessageService.save(offlineMessage);
    }

    @Override
    public void removeOfflineMsg(Message pack) {
        String msgId = pack.getId();
        QueryWrapper<OfflineMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(OfflineMessage::getMsgId, msgId);
        offlineMessageService.remove(queryWrapper);
    }

    @Override
    public boolean containsMsg(Message pack) {
        String msgId = pack.getId();
        QueryWrapper<OfflineMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(OfflineMessage::getMsgId, msgId);
        return offlineMessageService.getOne(queryWrapper) != null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<Message> getOfflineMsgByToId(String key) {
        List<OfflineMessage> list = offlineMessageService.lambdaQuery().eq(OfflineMessage::getToId, key).list();
        return list.stream().map(OfflineMessage::getMessage).collect(Collectors.toSet());
    }

    @Override
    public void clean() {
        QueryWrapper<OfflineMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().lt(OfflineMessage::getCreateTime, LocalDateTime.now().minusDays(1));
        List<OfflineMessage> list = offlineMessageService.list(queryWrapper);
        for (OfflineMessage offlineMessage : list) {
            Message offlineMsg = offlineMessage.getMessage();
            Message.Header header = new Message.Header();
            header.setFromId("server");
            String fromId = offlineMsg.getHeader().getFromId();
            header.setToId(fromId);
            header.setMsgType(MsgType.SEND);
            header.setStatus(MsgStatus.FAIL.getValue());
            Message message = new Message(header, offlineMsg.getBody());
            message.setOriginId(offlineMsg.getId());
            ClientInfo clientInfo = ClientContainer.getById(fromId);
            if (clientInfo != null) {
                transferMsg(message, clientInfo);
            }
        }
        offlineMessageService.remove(queryWrapper);
    }
}
