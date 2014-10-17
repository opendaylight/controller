package org.opendaylight.persisted.autoagents;

public interface IClusterMapListener<K,V> {
    public void peerPut(K key,V value);
    public void peerRemove(K key);
}
