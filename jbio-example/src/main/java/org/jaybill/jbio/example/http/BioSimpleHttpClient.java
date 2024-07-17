package org.jaybill.jbio.example.http;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

@Slf4j
public class BioSimpleHttpClient {

    public static void main(String[] args) throws IOException {
        // req
        var body = "jbio, hello world!";
        var bodyLen = body.getBytes(StandardCharsets.UTF_8).length;
        var req1 = String.format("POST /hello-world HTTP/1.1\r\nHost:127.0.0.1:8080\r\nContent-Length:%s\r\n\r\n%s",
                bodyLen, body);
        var req2 = String.format("POST /jbio HTTP/1.1\r\nHost:127.0.0.1:8080\r\nContent-Length:%s\r\n\r\n%s",
                bodyLen, body);

        // socket
        var socket = new Socket();
        socket.connect(new InetSocketAddress("127.0.0.1", 8080));
        var out = socket.getOutputStream();
        var in = socket.getInputStream();

        // write
        log.debug("Send -> {}", req1);
        out.write(req1.getBytes(StandardCharsets.UTF_8));

        int cnt = 0;
        // read
        var bs = new byte[128];
        int len;
        while ((len = in.read(bs)) != -1) {
            log.debug("Receive -> {}", new String(bs, 0, len));
            // continue send
            if (cnt++ % 2 == 0) {
                out.write(req1.getBytes(StandardCharsets.UTF_8));
                log.debug("Send -> {}", req1);
            } else {
                out.write(req2.getBytes(StandardCharsets.UTF_8));
                log.debug("Send -> {}", req2);
            }
        }
    }
}
