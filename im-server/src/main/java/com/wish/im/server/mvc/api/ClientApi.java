package com.wish.im.server.mvc.api;

import com.wish.im.server.netty.client.ClientContainer;
import com.wish.im.server.netty.client.ClientInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 描述
 *
 * @author shy
 * @since 2021/8/13
 */
@RequestMapping("/client")
@RestController
public class ClientApi {
    @GetMapping("/list")
    public List<ClientInfo> list() {
        return ClientContainer.getAllClients();
    }
}
