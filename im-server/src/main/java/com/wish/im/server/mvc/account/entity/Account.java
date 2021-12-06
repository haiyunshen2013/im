package com.wish.im.server.mvc.account.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * 描述
 *
 * @author shy
 * @since 2021/7/29
 */
@Data
public class Account {

    /**
     * id
     */
    private String id;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createTime;

    /**
     * 创建人
     */
    @TableField(fill = FieldFill.INSERT, updateStrategy = FieldStrategy.NEVER)
    private String creator;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 更新人
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;
    /**
     * remark
     */
    private String remark;
    /**
     * 身份
     */
    @NotBlank
    private String name;

    /**
     * 授权码
     */
    @NotBlank
    private String pwd;

    /**
     * 令牌过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 0:普通，1:管理员
     */
    private int type;
}
