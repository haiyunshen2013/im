package com.wish.im.server.mvc.message.data;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wish.im.server.mvc.message.entity.MessageLog;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 描述
 *
 * @author shy
 * @since 2021/8/11
 */
@Mapper
@Repository
public interface MessageLogMapper extends BaseMapper<MessageLog> {
}
