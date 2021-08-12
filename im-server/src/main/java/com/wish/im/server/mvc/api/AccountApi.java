package com.wish.im.server.mvc.api;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wish.im.server.mvc.account.entity.Account;
import com.wish.im.server.mvc.account.service.AccountService;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 描述
 *
 * @author shy
 * @since 2021/7/30
 */
@RestController
@RequestMapping("/account")
@AllArgsConstructor
public class AccountApi {
    private final AccountService accountService;

    @PostMapping("/pageList")
    public Page<Account> pageList(@RequestBody Account account, int pageNo, int pageSize) {
        QueryWrapper<Account> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(StringUtils.isNotBlank(account.getId()), Account::getId, account.getId())
                .like(StringUtils.isNotBlank(account.getName()), Account::getName, account.getName())
                .orderByDesc(Account::getUpdateTime);
        Page<Account> accountPage = new Page<>(pageNo, pageSize);
        accountService.page(accountPage, queryWrapper);
        return accountPage;
    }

    @PostMapping("/add")
    public Account add(@RequestBody @Valid Account account) {
        accountService.save(account);
        return account;
    }

    @PostMapping("/update")
    public Account update(@RequestBody Account account) {
        accountService.updateById(account);
        return account;
    }

    @GetMapping("/delete")
    public boolean delete(String id) {
        return accountService.removeById(id);
    }
}
