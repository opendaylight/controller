package org.opendaylight.controller.sal.binding.dom.serializer.api;

import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;

public interface ChoiceCaseCodec<C extends DataContainer> extends DataContainerCodec<C> {

    @Override
    public CompositeNode serialize(ValueWithQName<C> input);
    
    @Override
    public ValueWithQName<C> deserialize(Node<?> input);
    
    public boolean isAcceptable(Node<?> input);
}
