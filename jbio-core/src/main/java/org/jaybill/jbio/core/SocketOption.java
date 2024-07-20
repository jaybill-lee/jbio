package org.jaybill.jbio.core;

public class SocketOption<T> {
    public static final SocketOption<Boolean> TCP_NODELAY = new SocketOption<>();
    public static final SocketOption<Boolean> SO_KEEPALIVE = new SocketOption<>();
    public static final SocketOption<Integer> SO_SNDBUF = new SocketOption<>();
    public static final SocketOption<Integer> SO_RCVBUF = new SocketOption<>();
    public static final SocketOption<Boolean> SO_REUSEADDR = new SocketOption<>();
    public static final SocketOption<Boolean> SO_REUSE_PORT = new SocketOption<>();
    public static final SocketOption<Integer> SO_LINGER = new SocketOption<>();
    public static final SocketOption<Integer> SO_BACKLOG = new SocketOption<>();
}
