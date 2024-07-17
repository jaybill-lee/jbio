package org.jaybill.jbio.example.echo;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

@Slf4j
public class BioEchoClient {
    public static void main(String[] args) throws IOException {
        var msg = "hello world";
        var socket = new Socket();
        socket.connect(new InetSocketAddress("127.0.0.1", 8080));
        var out = socket.getOutputStream();
        var in = socket.getInputStream();
        out.write(msg.getBytes(StandardCharsets.UTF_8));
        log.debug("Send -> {}", msg);
        byte [] bs = new byte[128];
        int len;
        while ((len = in.read(bs)) != -1) {
            log.debug("Receive -> {}", new String(bs, 0, len));
            // send again
            out.write(msg.getBytes(StandardCharsets.UTF_8));
        }
    }
}
