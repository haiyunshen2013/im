# im/im_netty

## 开始

1. im是一个分为C端和S端的，采用netty作为底层的及时铜须框架，具有断线自动重连，支持同步等待响应，和异步接收通知的功能。 其中C端分为普通C端、具备管理员权限的C端(目前仅区分2种)，区别在于，普通C端只具备收发消息的功能。
   管理员C端拥有部分特殊功能，如获取当前所有在线的C端详细信息，可修改其他C端的认证信息，过期时间等功能。
2. S端采用MySql作为数据库存储消息收发日志，且处理具有缓存功能的消息(如果消息允许缓存，则当消息的目标接收端上线时，会立即发送之前缓存的消息)

## 消息简介

```java
public class Message {
   /**
    * 消息id
    */
   private String id;

   @Nullable
   private String originId;

   /**
    * 消息来源
    */
   private String fromId;

   /**
    * 消息目的
    */
   private String toId;

   /**
    * 消息类型
    *
    * @see MsgType
    */
   private int type = MsgType.SEND;

//    private long timestamp = System.currentTimeMillis();
   /**
    * @see MsgStatus
    */
   private int status;

   private String method;

   // 管理员功能入口，采用与http协议类似的做法，使用url与method调用具体的方法，兼容了springmvc 的注解
   private String url;

   /**
    * 是否允许服务端存储
    */
   private boolean enableCache;


   private byte[] body;
}
```

> 消息结构如上，其中body作为消息载荷，支持普通字符串与其他多媒体消息(默认大小受限，为100KB，受限于```com.wish.im.server.netty.bootstrap.NettyServerBootstrap```的消息帧编解码器)

## 示例

1. 服务端采用Spring Boot,直接启动```com.wish.im.server.ImServerApplication```

2. 客户端可嵌入其他Spring Boot服务，或Android端

   1. Android端示例代码如下

      ```java
      public class MainActivity extends AppCompatActivity {
          private EditText mEditText = null;
          private TextView mTextView = null;
      
          private static final String clientId = "3";
          private ImClient imClient;
      
          private final String token = "1234";
      
          private static final ExecutorService executor = Executors.newFixedThreadPool(1);
      
          @Override
          protected void onCreate(Bundle savedInstanceState) {
              super.onCreate(savedInstanceState);
              setContentView(R.layout.activity_main);
              setContentView(R.layout.activity_main);
      //        setSupportActionBar(findViewById(R.id.toolbar))
              imClient = new ImClient(clientId, "172.16.0.8", 8080);
              // 等待其他客户端发送的消息
              imClient.setCallback((message) -> runOnUiThread(() -> {
                          CharSequence text = mTextView.getText();
                          if (message.getBody() != null) {
                              text = new String(message.getBody(), StandardCharsets.UTF_8) + System.lineSeparator() + text;
                              mTextView.setText(text);
                          } else if (message.getType() == MsgType.ACK && message.getStatus() == MsgStatus.RECEIVER_ACK.getValue()) {
                              text = "\"" +mEditText.getText() + "\"消息已送达" + System.lineSeparator() + text;
                              mTextView.setText(text);
                              mEditText.setText("");
                          }
                      }
              ));
              executor.execute(() -> {
                  imClient.setToken(token);
                  imClient.setAutoReconnect(true);
                  imClient.setAutoHeart(true);
                  imClient.connect();
      //            pipeline.remove(JsonDecoder.class);
      //            pipeline.addAfter("frameDecoder","demoHandler",new DemoHandler());
              });
              mTextView = findViewById(R.id.tv_msg);
              mEditText = findViewById(R.id.et_content);
          }
      
          public void sendMsg(View view) {
              String msg = mEditText.getText().toString();
              Message message = Message.builder().type(MsgType.SEND).toId("1").body(msg.getBytes(StandardCharsets.UTF_8)).build();
              imClient.sendMsg(message);
          }
      }
      ```

   2. 其他java服务实例

      ```java
       @PostMapping("/demo/test")
          public ResponseModel<?> test(@RequestBody Map<String, Object> map) {
              Boolean enableCache = (Boolean) map.get("enableCache");
      
              Message.MessageBuilder messageBuilder = Message.builder().toId((String) map.get("toId"))
                      .method((String) map.get("method"))
                      .url((String) map.get("url"))
                      .type((Integer) map.get("msgType"));
              if (enableCache != null) {
                  messageBuilder.enableCache(enableCache);
              }
              Object body = map.get("body");
              byte[] bodyArr = null;
              if (body != null) {
                  if (body instanceof String) {
                      bodyArr = ((String) body).getBytes(StandardCharsets.UTF_8);
                  }else {
                      bodyArr = JsonUtils.serializeAsBytes(body);
                  }
              }
              Message message = messageBuilder.body(bodyArr).build();
              ListenableFuture<Message> future = client.sendMsg(message);
              Message data;
              try {
                  // 除了异步被动接收消息外，也可以直接在发送消息后获取当前消息发送的回执
                  data = future.get();
                  Object deserialize = JsonUtils.deserialize(data.getBody(), Object.class);
                  return ResponseModel.setSuccess(deserialize);
              } catch (InterruptedException | ExecutionException e) {
                  e.printStackTrace();
              }
              return ResponseModel.error(HttpStatus.BAD_REQUEST, null);
          }
      ```

## 管理消息示例

1. 获取所有客户端信息的消息

   ```json
   {
   	"enableCache": true, // 可不传，管理类型消息此字段无效
   	"toId": "3", // 可不传，管理类型消息此字段无效
   	"msgType": 1005, // 消息类型必传，
   	"method": "POST", // 和api接口RequestMapping注解的方法一致
   	"url": "/account/pageList?pageNo=1&pageSize=10",// url
       "body":{} // RequestBody里的参数
   }
   ```


# TODO

1. 增加grpc支持
2. 增加负载均衡与高可用性支持
