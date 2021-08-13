package com.wish.im.client.message;

import com.wish.im.client.concurrent.SettableListenableFuture;
import com.wish.im.common.message.Message;
import lombok.Data;

/**
 * 描述
 *
 * @author shy
 * @since 2021/8/13
 */
@Data
public class ResponseMessage {
    private Message originMessage;

    private SettableListenableFuture<Message> listenableFuture;

    private long timestamp = System.currentTimeMillis();

    public ResponseMessage(Message originMessage, SettableListenableFuture<Message> listenableFuture) {
        this.originMessage = originMessage;
        this.listenableFuture = listenableFuture;
    }
}
