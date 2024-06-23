package org.jaybill.jbio.core;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DefaultChannelPipeline implements ChannelPipeline {

    private final Lock lock = new ReentrantLock();
    private final DefaultChannelHandlerContext head;
    private final DefaultChannelHandlerContext tail;
    private final EventLoop eventLoop;

    public DefaultChannelPipeline(ChannelHandler headHandler, ChannelHandler tailHandler, EventLoop eventLoop) {
        this.eventLoop = eventLoop;
        head = new DefaultChannelHandlerContext(headHandler, eventLoop);
        tail = new DefaultChannelHandlerContext(tailHandler, eventLoop);
        head.next = tail;
        tail.prev = head;
    }

    @Override
    public ChannelPipeline addLast(ChannelHandler handler) {
        var cur = new DefaultChannelHandlerContext(handler, eventLoop);
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
    public void fireChannelActive() {
        ((ChannelInboundHandler) head.handler()).channelActive(head);
    }

    @Override
    public void fireChannelRead(ByteBuffer buf) {
        ((ChannelInboundHandler) head.handler()).channelRead(head, buf);
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
    public void fireChannelInactive() {
        ((ChannelInboundHandler) head.handler()).channelInactive(head);
    }

    @Override
    public void fireChannelException(Throwable t) {

    }

    @Override
    public CompletableFuture<Void> fireChannelWrite(ByteBuffer buf) {
        return null;
    }
}
