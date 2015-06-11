/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Map;
import org.opendaylight.controller.cluster.datastore.node.utils.serialization.NormalizedNodeSerializer;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages.Node;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;


class RemoteRpcInput implements ContainerNode {

    private final ContainerNode delegate;

    private RemoteRpcInput(final ContainerNode delegate) {
        this.delegate = delegate;
    }

    protected static RemoteRpcInput from(final Node node) {
        if(node == null) {
            return null;
        }
        final NormalizedNode<?, ?> deserialized = NormalizedNodeSerializer.deSerialize(node);
        Preconditions.checkArgument(deserialized instanceof ContainerNode);
        return new RemoteRpcInput((ContainerNode) deserialized);
    }

    ContainerNode delegate() {
        return delegate;
    }

    @Override
    public Map<QName, String> getAttributes() {
        return delegate().getAttributes();
    }

    @Override
    public Object getAttributeValue(final QName name) {
        return delegate().getAttributeValue(name);
    }

    @Override
    public QName getNodeType() {
        return delegate().getNodeType();
    }

    @Override
    public Collection<DataContainerChild<? extends PathArgument, ?>> getValue() {
        return delegate().getValue();
    }

    @Override
    public NodeIdentifier getIdentifier() {
        return delegate().getIdentifier();
    }

    @Override
    public Optional<DataContainerChild<? extends PathArgument, ?>> getChild(final PathArgument child) {
        return delegate().getChild(child);
    }

    @Override
    public int hashCode() {
        return delegate().hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return delegate().equals(obj);
    }
}
