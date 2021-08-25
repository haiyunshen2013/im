package com.wish.im.server.netty.scheduler;

import com.wish.im.server.netty.message.IOfflineMessageContainer;
import lombok.AllArgsConstructor;

/**
 * 定时调度类，执行定时清理资源
 *
 * @author shy
 * @since 2021/7/29
 */
//@Component
@AllArgsConstructor
public class ImServerScheduler {
    private final IOfflineMessageContainer offlineMessageContainer;

//    @Scheduled(cron = "0 0/1 * * * ?")
    public void cleanRubbishMsg() {
        offlineMessageContainer.clean();
    }
}
