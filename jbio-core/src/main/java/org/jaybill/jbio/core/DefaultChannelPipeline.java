package org.jaybill.jbio.core;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DefaultChannelPipeline implements ChannelPipeline {

    private final Lock lock = new ReentrantLock();
    private final DefaultChannelHandlerContext head;
    private final DefaultChannelHandlerContext tail;
    private final EventLoop eventLoop;
    private final NioChannel channel;

    public DefaultChannelPipeline(ChannelHandler headHandler, ChannelHandler tailHandler, NioChannel channel, EventLoop eventLoop) {
        this.eventLoop = eventLoop;
        this.channel = channel;
        head = new DefaultChannelHandlerContext(headHandler, channel, eventLoop);
        tail = new DefaultChannelHandlerContext(tailHandler, channel, eventLoop);
        head.next = tail;
        tail.prev = head;
    }

    @Override
    public ChannelPipeline addLast(ChannelHandler handler) {
        var cur = new DefaultChannelHandlerContext(handler, channel, eventLoop);
        try {
            lock.lock();
            var p = tail.prev;
            p.next = cur;
            cur.prev = p;
            cur.next = tail;
            tail.prev = cur;
            return this;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public ChannelPipeline remove(ChannelHandler handler) {
        try {
            lock.lock();
            var cur = head.next;
            while (cur != tail) {
                if (handler == cur.handler()) {
                    // We only remove the node from pipeline,
                    // and still reserve the prev and next pointer of the node.
                    var prev = cur.prev;
                    var next = cur.next;
                    prev.next = next;
                    next.prev = prev;
                    break;
                } else {
                    cur = cur.next;
                }
            }
        } finally {
            lock.unlock();
        }
        return this;
    }

    @Override
    public void fireChannelInitialized() {
        ((ChannelInboundHandler) head.handler()).channelInitialized(head);
    }

    @Override
    public void fireChannelBound() {
        ((ChannelInboundHandler) head.handler()).channelBound(head);
    }

    @Override
    public void fireChannelRegistered() {
        ((ChannelInboundHandler) head.handler()).channelRegistered(head);
    }

    @Override
    public void fireChannelRead(Object o) {
        ((ChannelInboundHandler) head.handler()).channelRead(head, o);
    }

    @Override
    public void fireChannelClosed() {
        ((ChannelInboundHandler) head.handler()).channelClosed(head);
    }

    @Override
    public void fireChannelDeregistered() {
        ((ChannelInboundHandler) head.handler()).channelDeregistered(head);
    }

    @Override
    public void fireChannelException(Throwable t) {
        ((ChannelInboundHandler) head.handler()).channelException(head, t);
    }

    @Override
    public void fireChannelUnWritable() {
        ((ChannelInboundHandler) head.handler()).channelUnWritable(head);
    }

    @Override
    public void fireChannelWritable() {
        ((ChannelInboundHandler) head.handler()).channelWritable(head);
    }

    @Override
    public void fireChannelClose() {
        ((ChannelOutboundHandler) tail.handler()).close(tail);
    }

    @Override
    public CompletableFuture<Void> fireChannelWrite(Object buf) {
        return ((ChannelOutboundHandler) tail.handler()).write(tail, buf);
    }

    @Override
    public CompletableFuture<Void> fireChannelFlush() {
        return ((ChannelOutboundHandler) tail.handler()).flush(tail);
    }

    @Override
    public CompletableFuture<Void> fireChannelWriteAndFlush(Object buf) {
        return ((ChannelOutboundHandler) tail.handler()).writeAndFlush(tail, buf);
    }
}
