package org.opendaylight.datasand.agents.cluster;

import org.opendaylight.datasand.codec.TypeDescriptorsContainer;

public class PeerListener<K,V> implements IClusterMapListener<K, V>{
    private TypeDescriptorsContainer container = null;
    public PeerListener(TypeDescriptorsContainer _container){
        this.container = _container;
    }
    @Override
    public void peerPut(K key, V value) {
        container.save();
    }
    @Override
    public void peerRemove(K key) {
        container.save();
    }
}
