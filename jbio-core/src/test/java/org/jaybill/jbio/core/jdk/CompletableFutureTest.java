package org.jaybill.jbio.core.jdk;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class CompletableFutureTest {

    @Test
    public void testCompletableFuture_IsCancel() throws InterruptedException {
        var cdl = new CountDownLatch(1);
        var future = new CompletableFuture<>();
        var thread = new Thread(() -> {
            future.cancel(true);
            cdl.countDown();
        });
        thread.start();
        cdl.await();
        Assert.assertTrue(future.isCancelled());
    }

    @Test
    public void testCompletableFuture_Success_Then_Cancel() {
        var future = new CompletableFuture<>();
        // normal success
        future.complete(null);
        Assert.assertTrue(future.isDone()); // done
        Assert.assertFalse(future.isCancelled()); // is not cancelled
        Assert.assertFalse(future.isCompletedExceptionally()); // no exception

        // try to cancel
        future.cancel(false);
        Assert.assertTrue(future.isDone()); // done
        Assert.assertFalse(future.isCancelled()); // is not cancelled
        Assert.assertFalse(future.isCompletedExceptionally()); // no exception
    }
}
