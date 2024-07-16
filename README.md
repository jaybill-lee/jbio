The `jbio` is a network programming framework based on `NIO`. <br>
It draws on the design concept of `netty` and provides a high-performance, easy-to-use, and highly scalable programming interface.<br>
The project only requires `JDK21` to run.
<br><br>
**Note**: This project is primarily intended for learning and experimentation.

## Features
- TCP
- HTTP/1.1
- HTTP2 (planning)
- MQTT (planning)
- RESP (planning)

## Example
Step1, create an HTTP server
```
public static void main(String[] args) {
    // Create a jbio server instance
    var server = JBIOServer.newInstance()
          .config(NioChannelConfigTemplate.DEFAULT, NioSocketChannelConfigTemplate.DEFAULT)
          .initializer(null, channel -> {
              channel.pipeline()
                .addLast(new HttpServerCodecHandler())
                .addLast(new DefaultChannelDuplexHandler() {
                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object o) {
                        var pair = (HttpPair) o;
                        var resp = pair.getResponse();
                        resp.setVersion(HttpVersion.HTTP1_1);
                        resp.setStatusCode(GeneralStatusCode.OK.getCode());
                        resp.setReasonPhrase(GeneralStatusCode.OK.getReasonPhrase());
                        resp.setBody("hello world".getBytes(StandardCharsets.UTF_8));
                        ctx.channel().pipeline().fireChannelWriteAndFlush(resp);
                    }
                });
          })
        .eventLoop(1, Runtime.getRuntime().availableProcessors());
    // Start server
    server.start("127.0.0.1", 8080).join();
}
```
Step2, request the server
```
curl http://127.0.0.1:8080/hello
```

