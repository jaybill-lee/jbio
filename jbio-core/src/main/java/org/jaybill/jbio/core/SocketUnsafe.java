package org.jaybill.jbio.core;

public interface SocketUnsafe extends Unsafe {

    void connect();

    void read();

    void write();
}
