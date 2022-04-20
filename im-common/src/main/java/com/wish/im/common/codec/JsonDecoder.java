package com.wish.im.common.codec;

import com.wish.im.common.message.Message;
import com.wish.im.common.util.JsonUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.nio.charset.StandardCharsets;
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
        ByteBuf byteBuf = in.slice(0, 8);
        in.readerIndex(8);
        byte[] magicBytes = ByteBufUtil.getBytes(byteBuf, byteBuf.readerIndex(), byteBuf.readableBytes(), false);
        String magic = new String(magicBytes, StandardCharsets.UTF_8); // magic num
        byte version = in.readByte(); // codec version
        final byte[] array;
        final int length = in.readableBytes();
        array = ByteBufUtil.getBytes(in, in.readerIndex(), length, false);
        out.add(JsonUtils.deserialize(array, Message.class));
    }
}
