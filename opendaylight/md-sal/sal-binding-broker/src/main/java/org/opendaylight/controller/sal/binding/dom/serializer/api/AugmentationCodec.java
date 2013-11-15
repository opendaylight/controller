package org.opendaylight.controller.sal.binding.dom.serializer.api;

import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;

public interface AugmentationCodec<A extends Augmentation<?>> extends DomCodec<A> {

    
    @Override
    public CompositeNode serialize(ValueWithQName<A> input);
    
    @Override
    public ValueWithQName<A> deserialize(Node<?> input);
}
