package org.openflow.util;

import java.util.LinkedHashMap;

public class LRULinkedHashMap<K, V> extends LinkedHashMap<K, V> {
    private static final long serialVersionUID = -2964986094089626647L;
    protected int maximumCapacity;

    public LRULinkedHashMap(int initialCapacity, int maximumCapacity) {
        super(initialCapacity, 0.75f, true);
        this.maximumCapacity = maximumCapacity;
    }

    public LRULinkedHashMap(int maximumCapacity) {
        super(16, 0.75f, true);
        this.maximumCapacity = maximumCapacity;
    }

    @Override
    protected boolean removeEldestEntry(java.util.Map.Entry<K, V> eldest) {
        if (this.size() > maximumCapacity)
            return true;
        return false;
    }
}
