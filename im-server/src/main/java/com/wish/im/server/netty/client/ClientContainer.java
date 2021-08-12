package com.wish.im.server.netty.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端容器
 *
 * @author shy
 * @since 2021/7/26
 */
public class ClientContainer {
    private static final Map<String, ClientInfo> CACHE = new ConcurrentHashMap<>();

    public static void add(ClientInfo clientInfo) {
        CACHE.put(clientInfo.getId(), clientInfo);
    }

    public static List<ClientInfo> getAllClients() {
        List<ClientInfo> result = new ArrayList<>();
        for (Map.Entry<String, ClientInfo> entry : CACHE.entrySet()) {
            result.add(entry.getValue());
        }
        return result;
    }

    public static ClientInfo getById(String id) {
        return CACHE.get(id);
    }


    public static void removeById(String id) {
        CACHE.remove(id);
    }
}
