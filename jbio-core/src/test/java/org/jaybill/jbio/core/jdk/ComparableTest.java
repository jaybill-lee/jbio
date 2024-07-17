package org.jaybill.jbio.core.jdk;

import lombok.AllArgsConstructor;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.PriorityBlockingQueue;

public class ComparableTest {

    @Test
    public void testComparable() {
        var queue = new PriorityBlockingQueue<T>();
        queue.add(new T(3));
        queue.add(new T(1));
        queue.add(new T(2));

        T t;
        int i = 1;
        while ((t = queue.poll()) != null) {
            Assert.assertEquals(i++, t.i);
        }
    }

    @AllArgsConstructor
    public static class T implements Comparable<T> {
        private int i;

        @Override
        public int compareTo(T o) {
            int x = this.i - o.i;
            if (x < 0) {
                return -1;
            } else if (x == 0){
                return 0;
            } else {
                return 1;
            }
        }
    }
}
