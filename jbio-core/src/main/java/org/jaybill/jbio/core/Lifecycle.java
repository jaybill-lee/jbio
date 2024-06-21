package org.jaybill.jbio.core;

/**
 * The lifecycle of a channel includes three scenarios: <br/>
 * 1. init -> bind -> register -> active -> close -> inactive -> deregister <br/>
 * 2. init -> bind -> register -> active -> inactive -> deregister <br/>
 * 3. init -> bind -> register -> deregister
 */
public interface Lifecycle {
    void init();
    void bind();
    void register();
    void active();
    void close();
    void inactive();
    void deregister();
}
