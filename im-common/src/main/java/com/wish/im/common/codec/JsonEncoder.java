package com.wish.im.common.codec;

import com.wish.im.common.message.Message;
import com.wish.ipusher.api.utils.JsonUtils;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

/**
 * 描述
 *
 * @author shy
 * @since 2021/7/22
 */
@ChannelHandler.Sharable
public class JsonEncoder extends MessageToMessageEncoder<Message> {
    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, List<Object> out) throws Exception {
        out.add(Unpooled.wrappedBuffer(JsonUtils.serializeAsBytes(msg)));
    }
}
