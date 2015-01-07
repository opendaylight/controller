package org.opendaylight.datasand.agents.cmap;

import org.opendaylight.datasand.codec.TypeDescriptorsContainer;

public class TypeDescriptorListener<K,V> implements ICMapListener<K, V>{
    private TypeDescriptorsContainer container = null;
    public TypeDescriptorListener(TypeDescriptorsContainer _container){
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
