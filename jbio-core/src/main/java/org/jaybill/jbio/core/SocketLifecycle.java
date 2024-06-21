package org.jaybill.jbio.core;

import java.nio.ByteBuffer;

public interface SocketLifecycle extends Lifecycle {

    void connect();

    void read();

    void write();
}
