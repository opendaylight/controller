package org.opendaylight.controller.sal.binding.dom.serializer.api;

import org.opendaylight.yangtools.yang.binding.BindingCodec;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.Node;

public interface DomCodec<I> extends BindingCodec<Node<?>, org.opendaylight.controller.sal.binding.dom.serializer.api.ValueWithQName<I>>{


    @Override
    public Node<?> serialize(org.opendaylight.controller.sal.binding.dom.serializer.api.ValueWithQName<I> input);


    @Override
    public org.opendaylight.controller.sal.binding.dom.serializer.api.ValueWithQName<I> deserialize(Node<?> input);

}
