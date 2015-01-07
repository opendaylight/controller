package org.opendaylight.datasand.agents.cluster;

public interface IClusterMapListener<K,V> {
    public void peerPut(K key,V value);
    public void peerRemove(K key);
}
