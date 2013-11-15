package org.opendaylight.controller.sal.binding.dom.serializer.api;

import org.opendaylight.yangtools.yang.data.api.Node;

public interface ChoiceCodec<C> extends DomCodec<C> {

    @Override
    public Node<?> serialize(ValueWithQName<C> input);
    
    @Override
    public ValueWithQName<C> deserialize(Node<?> input);
}
