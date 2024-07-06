package org.jaybill.jbio.core;

/**
 * The lifecycle of a channel includes three scenarios: <br/>
 * 1. init -> bind -> register -> close -> deregister <br/>
 * 2. init -> bind -> register -> deregister <br/>
 */
public interface Unsafe {
    void init();
    void bind();
    void register();
    void close();
    void deregister();
}
