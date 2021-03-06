package com.wish.im.client;

import com.wish.im.client.concurrent.ListenableFuture;
import com.wish.im.common.message.Message;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * 描述
 *
 * @author shy
 * @since 2021/7/26
 */
public class ClientTest {
    @Test
    public void f1() throws InterruptedException {
        ImClient client = createClient("1");
        client.setToken("123");
        client.setAutoHeart(true);
        client.setAutoReconnect(true);
        client.connect();
        sendMsg("2", client, true);
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
        ImClient client = createClient("2");
        client.setToken("123");
        client.setAutoHeart(true);
        client.connect();
        sendMsg("1", client, false);
        Thread.currentThread().join();
    }

    private void createClient(String id, String toId) throws InterruptedException {
        ImClient nettyClient = createClient(id);
        sendMsg(toId, nettyClient, false);
    }

    private void sendMsg(String toId, ImClient nettyClient, boolean enableCache) throws InterruptedException {
        new Thread(() -> {
            Scanner sc = new Scanner(System.in);
            while (sc.hasNext()) {
                try {
                    String str = sc.nextLine();
                    String[] s = str.split(" ");
                    String bodyStr = null;
                    if (s.length > 3) {
                        String[] body = new String[s.length - 3];
                        System.arraycopy(s, 3, body, 0, body.length);
                        bodyStr = String.join(" ", body);
                    }
                    Message message = Message.builder().toId(toId)
                            .method(s[0])
                            .url(s[1])
                            .type(Integer.parseInt(s[2]))
                            .body(bodyStr == null ? null : bodyStr.getBytes(StandardCharsets.UTF_8)).build();
                    if (enableCache) {
                        message.setEnableCache(true);
                    }
                    ListenableFuture<Message> listenableFuture = nettyClient.sendMsg(message);
                    TimeUnit.SECONDS.sleep(1);
                    System.err.println(listenableFuture);
                } catch (NumberFormatException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @NotNull
    private static ImClient createClient(String id) {
        return new ImClient(id, "localhost", 8080);
    }

    @Test
    public void f3() {
        Message message = Message.builder().toId("toId").body("body".getBytes(StandardCharsets.UTF_8)).build();
        message.setFromId("1");
        System.err.println(message);
    }
}