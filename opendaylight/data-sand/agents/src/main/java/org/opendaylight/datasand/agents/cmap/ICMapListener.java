package org.opendaylight.datasand.agents.cmap;

public interface ICMapListener<K,V> {
    public void peerPut(K key,V value);
    public void peerRemove(K key);
}
