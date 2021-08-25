package com.wish.im.server.mvc.message.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wish.im.common.message.Message;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 持久化消息
 *
 * @author shy
 * @since 2021/8/11
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName(autoResultMap = true)
public class MessageLog extends Message {

    private String fromChannelId;

    private String toChannelId;

    private String remoteAddress;

    @TableField("is_success")
    private boolean success;

    @TableField(
            fill = FieldFill.INSERT,
            updateStrategy = FieldStrategy.NEVER
    )
    private LocalDateTime createTime;
    @TableField(
            fill = FieldFill.INSERT,
            updateStrategy = FieldStrategy.NEVER
    )
    private String creator;
    @TableField(
            fill = FieldFill.INSERT_UPDATE
    )
    private LocalDateTime updateTime;
    @TableField(
            fill = FieldFill.INSERT_UPDATE
    )
    private String updatedBy;

    private String remark;
}
