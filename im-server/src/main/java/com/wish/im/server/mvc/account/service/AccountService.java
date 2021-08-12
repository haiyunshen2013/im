package com.wish.im.server.mvc.account.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wish.im.server.mvc.account.data.AccountMapper;
import com.wish.im.server.mvc.account.entity.Account;
import org.springframework.stereotype.Service;

/**
 * 描述
 *
 * @author shy
 * @since 2021/7/29
 */
@Service
public class AccountService extends ServiceImpl<AccountMapper, Account> {
}
