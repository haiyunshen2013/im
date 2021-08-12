package com.wish.im.client;

import com.wish.im.common.message.Message;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * 描述
 *
 * @author shy
 * @since 2021/7/26
 */
public class ClientTest {
    @Test
    public void f1() throws InterruptedException {
        NettyClient client = createClient("1");
        client.setToken("123");
        client.connect();
        client.setAutoHeart(true);
        client.setAutoReconnect(true);
        sendMsg("2", client, false);
        extracted();
        Thread.currentThread().join();
    }

    private void extracted() {
        ThreadGroup currentGroup =
                Thread.currentThread().getThreadGroup();
        int noThreads = currentGroup.activeCount();
        Thread[] lstThreads = new Thread[noThreads];
        currentGroup.enumerate(lstThreads);
        for (int i = 0; i < noThreads; i++)
            System.out.println("线程号：" + i + " = " + lstThreads[i].getName());
    }

    @Test
    public void f2() throws InterruptedException {
        NettyClient client = createClient("2");
        client.setToken("123");
        client.setAutoHeart(true);
        client.connect();
        sendMsg("1", client, false);
        Thread.currentThread().join();
    }

    private void createClient(String id, String toId) throws InterruptedException {
        NettyClient nettyClient = createClient(id);
        sendMsg(toId, nettyClient, false);
    }

    private void sendMsg(String toId, NettyClient nettyClient, boolean enableCache) throws InterruptedException {
        new Thread(() -> {
            Scanner sc = new Scanner(System.in);
            while (sc.hasNext()) {
                try {
                    String str = sc.nextLine();
                    String[] s = str.split(" ");
                    Message.Header header = new Message.Header();
                    header.setToId(toId);
                    header.setMethod(s[0]);
                    header.setUrl(s[1]);
                    header.setMsgType(Integer.parseInt(s[2]));
                    String bodyStr = null;
                    if (s.length > 3) {
                        String[] body = new String[s.length - 3];
                        System.arraycopy(s, 3, body, 0, body.length);
                        bodyStr = String.join(" ", body);
                    }
                    Message message = new Message(header, bodyStr == null ? null : bodyStr.getBytes(StandardCharsets.UTF_8));
                    if (enableCache) {
                        header.setEnableCache(true);
                    }
                    nettyClient.sendMsg(message);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @NotNull
    private static NettyClient createClient(String id) {
        return new NettyClient(id, "localhost", 8080);
    }
}