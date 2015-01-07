package org.opendaylight.datasand.agents.clustermap1;

public interface IClusterMapListener<K,V> {
    public void peerPut(K key,V value);
    public void peerRemove(K key);
}
