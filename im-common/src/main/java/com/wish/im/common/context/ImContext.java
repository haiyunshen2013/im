package com.wish.im.common.context;

import lombok.Data;

import java.util.UUID;

/**
 * 描述
 *
 * @author shy
 * @date 2020/10/15
 */
@Data
public class ImContext {
    private String id = UUID.randomUUID().toString().replaceAll("-", "");
    /**
     * 是否是超级管理员
     */
    private boolean isAdmin;
    /**
     * 用户名
     */
    private String uid;

    /**
     * 当前角色id
     */
    private String curRoleId;

    /**
     * 服务id
     */
    private String serviceId;

    /**
     * 组织id
     */
    private String orgId;

    /**
     * 是否是虚拟用户
     */
    private boolean isVirtual;
}
