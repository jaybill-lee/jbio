package org.jaybill.jbio.core;

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
        eventLoop.submitTask(() -> {
            ((ChannelInboundHandler) head.handler()).channelInitialized(head);
            return null;
        });
    }

    @Override
    public void fireChannelBound() {
        eventLoop.submitTask(() -> {
            ((ChannelInboundHandler) head.handler()).channelBound(head);
            return null;
        });
    }

    @Override
    public void fireChannelRegistered() {
        eventLoop.submitTask(() -> {
            ((ChannelInboundHandler) head.handler()).channelRegistered(head);
            return null;
        });
    }

    @Override
    public void fireChannelConnected() {
        eventLoop.submitTask(() -> {
            ((ChannelInboundHandler) head.handler()).channelConnected(head);
            return null;
        });
    }

    @Override
    public void fireChannelRead(Object o) {
        eventLoop.submitTask(() -> {
            ((ChannelInboundHandler) head.handler()).channelRead(head, o);
            return null;
        });
    }

    @Override
    public void fireChannelClosed() {
        eventLoop.submitTask(() -> {
            ((ChannelInboundHandler) head.handler()).channelClosed(head);
            return null;
        });
    }

    @Override
    public void fireChannelDeregistered() {
        eventLoop.submitTask(() -> {
            ((ChannelInboundHandler) head.handler()).channelDeregistered(head);
            return null;
        });
    }

    @Override
    public void fireChannelException(Throwable t) {
        eventLoop.submitTask(() -> {
            ((ChannelInboundHandler) head.handler()).channelException(head, t);
            return null;
        });
    }

    @Override
    public void fireChannelUnWritable() {
        eventLoop.submitTask(() -> {
            ((ChannelInboundHandler) head.handler()).channelUnWritable(head);
            return null;
        });
    }

    @Override
    public void fireChannelWritable() {
        eventLoop.submitTask(() -> {
            ((ChannelInboundHandler) head.handler()).channelWritable(head);
            return null;
        });
    }

    @Override
    public void fireChannelSendBufferFull() {
        eventLoop.submitTask(() -> {
            ((ChannelInboundHandler) head.handler()).channelSendBufferFull(head);
            return null;
        });
    }

    @Override
    public void fireChannelClose() {
        eventLoop.submitTask(() -> {
            ((ChannelOutboundHandler) tail.handler()).close(tail);
            return null;
        });
    }

    @Override
    public void fireChannelWrite(Object buf) {
        eventLoop.submitTask(() -> {
            ((ChannelOutboundHandler) tail.handler()).write(tail, buf);
            return null;
        });
    }

    @Override
    public void fireChannelFlush() {
        eventLoop.submitTask(() -> {
            ((ChannelOutboundHandler) tail.handler()).flush(tail);
            return null;
        });
    }

    @Override
    public void fireChannelWriteAndFlush(Object buf) {
        eventLoop.submitTask(() -> {
            ((ChannelOutboundHandler) tail.handler()).writeAndFlush(tail, buf);
            return null;
        });
    }
}
