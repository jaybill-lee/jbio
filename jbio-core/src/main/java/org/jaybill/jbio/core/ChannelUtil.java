package org.jaybill.jbio.core;

import java.io.IOException;
import java.nio.channels.SelectableChannel;

public class ChannelUtil {

    public static void forceClose(SelectableChannel ch) {
        if (ch == null) {
            return;
        }
        try {
            ch.close();
        } catch (IOException e) {
            // log
        }
    }
}
