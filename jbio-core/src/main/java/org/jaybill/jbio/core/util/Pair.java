package org.jaybill.jbio.core.util;

public class Pair<K, V> {
    private K left;
    private V right;

    private Pair() {}

    private Pair(K k, V v) {
        this.left = k;
        this.right = v;
    }

    public static <K, V> Pair<K, V> of(K k, V v) {
        return new Pair<>(k, v);
    }

    public K left() {
        return left;
    }

    public V right() {
        return right;
    }
}
