package com.wish.im.server.netty.message;

import com.wish.im.common.message.Message;
import lombok.Data;
import org.springframework.util.MultiValueMap;

import java.net.URI;

/**
 * 描述
 *
 * @author shy
 * @since 2021/8/11
 */
@Data
public class ReqMessage {
    private Message message;

    private Message.Header header;

    private URI uri;

    private MultiValueMap<String, String> queryParams;

    public void setMessage(Message message) {
        this.message = message;
        this.header = message.getHeader();
    }
}
