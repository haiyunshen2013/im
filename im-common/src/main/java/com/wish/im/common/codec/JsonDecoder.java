package com.wish.im.common.codec;

import com.wish.im.common.message.Message;
import com.wish.im.common.util.JsonUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

/**
 * 描述
 *
 * @author shy
 * @date 2021/7/22
 */
@ChannelHandler.Sharable
public class JsonDecoder extends MessageToMessageDecoder<ByteBuf> {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        final byte[] array;
        final int offset;
        final int length = in.readableBytes();
        if (in.hasArray()) {
            array = in.array();
            offset = in.arrayOffset() + in.readerIndex();
        } else {
            array = ByteBufUtil.getBytes(in, in.readerIndex(), length, false);
            offset = 0;
        }
        byte[] dest = new byte[length];
        System.arraycopy(array, offset, dest, 0, length);
        out.add(JsonUtils.deserialize(dest, Message.class));
    }
}
