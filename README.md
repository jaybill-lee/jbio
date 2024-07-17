The `jbio` is a network programming framework based on `NIO`. It is still under intensive development and improvement.<br>
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
[more example](https://github.com/jaybill-lee/jbio/tree/master/jbio-example/src/main/java/org/jaybill/jbio/example)
<br>
<br>
Step1, create an HTTP server
```
public static void main(String[] args) {
    JBIOServer.newInstance()
        .config(NioChannelConfigTemplate.DEFAULT, NioSocketChannelConfigTemplate.DEFAULT)
        .eventLoop(1, Runtime.getRuntime().availableProcessors())
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
        .start("127.0.0.1", 8080)
        .join();
}
```
Step2, request the server
```
curl http://127.0.0.1:8080/hello
```






