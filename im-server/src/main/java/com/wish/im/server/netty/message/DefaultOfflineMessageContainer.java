package com.wish.im.server.netty.message;

import com.wish.im.common.message.Message;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.wish.im.common.message.MsgType.HEART;

/**
 * 描述
 *
 * @author shy
 * @since 2021/7/27
 */
public class DefaultOfflineMessageContainer implements IOfflineMessageContainer {
    private static final Map<String, Set<Message>> OFFLINE = new ConcurrentHashMap<>();

    @Override
    public void putOffLienMsg(Message pack) {
        String toId = pack.getHeader().getToId();
        if (pack.getHeader().getMsgType() == HEART) {
            return;
        }
        if (containsMsg(pack)) {
            return;
        }
        if (OFFLINE.containsKey(toId)) {
            Set<Message> list = OFFLINE.get(toId);
            list.add(pack);
        } else {
            Set<Message> list = new LinkedHashSet<>();
            list.add(pack);
            OFFLINE.put(toId, list);
        }
    }

    @Override
    public void removeOfflineMsg(Message pack) {
        String toId = pack.getHeader().getToId();
        if (OFFLINE.containsKey(toId)) {
            OFFLINE.get(toId).remove(pack);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<Message> getOfflineMsgByToId(String key) {
        if (OFFLINE.containsKey(key)) {
            return OFFLINE.get(key);
        }
        return new HashSet<>();
    }

    @Override
    public void clean() {
        Set<Map.Entry<String, Set<Message>>> entries = OFFLINE.entrySet();
        Iterator<Map.Entry<String, Set<Message>>> iterator = entries.iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Set<Message>> entry = iterator.next();
            Set<Message> messages = entry.getValue();
            if (CollectionUtils.isEmpty(messages)) {
                iterator.remove();
            }
            Set<Message> remains = new LinkedHashSet<>();
            for (Message message : messages) {
                if (System.currentTimeMillis() - message.getHeader().getTimestamp() < 24 * 60 * 60 * 1000) {
                    remains.add(message);
                }
            }
            entry.setValue(remains);
        }
    }

    @Override
    public boolean containsMsg(Message pack) {
        Set<Message> messages = OFFLINE.get(pack.getHeader().getToId());
        if (CollectionUtils.isNotEmpty(messages)) {
            Optional<Message> any = messages.stream().filter(message -> StringUtils.equals(message.getId(), pack.getId())).findAny();
            return any.isPresent();
        }
        return false;
    }
}
