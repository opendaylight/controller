package org.opendaylight.datasand.agents.clustermap2;

public interface ICMapListener<K,V> {
    public void peerPut(K key,V value);
    public void peerRemove(K key);
}
