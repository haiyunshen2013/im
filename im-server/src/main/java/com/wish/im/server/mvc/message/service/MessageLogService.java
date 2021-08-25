package com.wish.im.server.mvc.message.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wish.im.server.mvc.message.data.MessageLogMapper;
import com.wish.im.server.mvc.message.entity.MessageLog;
import org.springframework.stereotype.Service;

/**
 * 描述
 *
 * @author shy
 * @since 2021/8/11
 */
@Service
public class MessageLogService extends ServiceImpl<MessageLogMapper, MessageLog> {
}
