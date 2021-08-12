package com.wish.im.server.mvc.account.entity;

import com.wish.ipusher.api.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * 描述
 *
 * @author shy
 * @since 2021/7/29
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class Account extends BaseEntity<String> {

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
