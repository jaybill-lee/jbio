package org.jaybill.jbio.core.jdk;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.channels.spi.SelectorProvider;

public class SelectorTest {

    /**
     * After a channel is registered, the `keys()` method of the selector can immediately retrieve it.
     */
    @Test
    public void testRegister() throws IOException {
        var provider = SelectorProvider.provider();
        var selector = provider.openSelector();
        var channel = provider.openSocketChannel();
        channel.configureBlocking(false);
        channel.register(selector, 0);
        Assert.assertTrue(selector.keys().contains(channel.keyFor(selector)));
    }
}
