package org.jaybill.jbio.core;

import lombok.Data;

import java.util.Map;

@Data
public class NioChannelConfig {
    private Map<SocketOption<?>, Object> options;
}
