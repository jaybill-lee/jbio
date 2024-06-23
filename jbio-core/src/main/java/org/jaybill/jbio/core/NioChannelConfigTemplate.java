package org.jaybill.jbio.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NioChannelConfigTemplate implements ChannelConfigTemplate {
    protected Map<SocketOption<?>, Object> options;
    public static NioChannelConfigTemplate DEFAULT = new NioChannelConfigTemplate();

    @Override
    public NioChannelConfig create() {
        var config = new NioChannelConfig();
        var optionMap = new ConcurrentHashMap<SocketOption<?>, Object>();
        config.setOptions(optionMap);
        if (options != null) {
            optionMap.putAll(options);
        }
        return config;
    }
}
