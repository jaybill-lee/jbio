package org.jaybill.jbio.core;

public interface NioChannelInitializer {
    <C extends NioChannel> void initChannel(C channel);
}
