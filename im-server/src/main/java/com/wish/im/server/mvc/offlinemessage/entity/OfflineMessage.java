package com.wish.im.server.mvc.offlinemessage.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.wish.im.common.message.Message;
import com.wish.ipusher.api.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 持久化消息
 *
 * @author shy
 * @since 2021/8/11
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName(autoResultMap = true)
public class OfflineMessage extends BaseEntity<String> {

    /**
     * 消息目的
     */
    private String toId;

    /**
     * 消息id
     */
    private String msgId;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Message message;
}
