package org.jaybill.jbio.core;

import lombok.Data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class NioChannelConfig {
    private Map<SocketOption<?>, Object> options;
    public static NioChannelConfig DEFAULT = new NioChannelConfig();
    static {
        DEFAULT.setOptions(new ConcurrentHashMap<>());
    }
}
