package org.jaybill.jbio.core;

import java.nio.channels.SelectableChannel;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractNioChannel {
    abstract void handleIOEvent();
}
