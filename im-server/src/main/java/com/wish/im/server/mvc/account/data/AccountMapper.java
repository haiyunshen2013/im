package com.wish.im.server.mvc.account.data;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wish.im.server.mvc.account.entity.Account;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 描述
 *
 * @author shy
 * @since 2021/7/29
 */
@Repository
@Mapper
public interface AccountMapper extends BaseMapper<Account> {
}
