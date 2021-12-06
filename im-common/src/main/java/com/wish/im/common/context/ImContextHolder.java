package com.wish.im.common.context;

import com.wish.im.common.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 描述
 *
 * @author shy
 * @date 2020/10/15
 */
public class ImContextHolder {
    private static final Logger log = LoggerFactory.getLogger(ImContextHolder.class);
    private static final ThreadLocal<ImContext> LOCAL = ThreadLocal.withInitial(ImContext::new);

    public static void setContext(ImContext ipusherContext) {
        LOCAL.set(ipusherContext);
    }

    public static ImContext currentContext() {
        return LOCAL.get();
    }

    public static void release() {
        log.debug("release : {}", JsonUtils.serialize(LOCAL.get()));
        LOCAL.remove();
    }
}
