package com.wish.im.server.netty.message;

import com.wish.im.common.message.Message;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.util.MultiValueMap;

import java.net.URI;

/**
 * 描述
 *
 * @author shy
 * @since 2021/8/11
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class RequestMessage extends Message {

    private URI uri;

    private MultiValueMap<String, String> queryParams;
}
