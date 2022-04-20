package com.wish.im.common.codec;

import com.wish.im.common.message.Message;
import com.wish.im.common.util.JsonUtils;
import io.netty.buffer.ByteBuf;
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
        ByteBuf buffer = ctx.alloc().buffer();
        byte[] bytes = {'B','I','G','B','A','B','A','Y'};// 8 bytes magic num
        buffer.writeBytes(bytes);
        buffer.writeByte(1);// 1 byte version
        buffer.writeBytes(JsonUtils.serializeAsBytes(msg));
        out.add(buffer);
    }
}
