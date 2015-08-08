/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.binding.impl;

import com.google.common.base.Optional;
import java.util.Collection;
import java.util.Map;
import org.opendaylight.yangtools.binding.data.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

/**
 *
 * FIXME: Should be this moved to binding-data-codec?
 *
 */
class LazySerializedContainerNode implements ContainerNode {

    private final NodeIdentifier identifier;
    private final DataObject bindingData;

    private BindingNormalizedNodeCodecRegistry registry;
    private ContainerNode domData;

    private LazySerializedContainerNode(final QName identifier, final DataObject binding,
            final BindingNormalizedNodeCodecRegistry registry) {
        this.identifier = new NodeIdentifier(identifier);
        this.bindingData = binding;
        this.registry = registry;
        this.domData = null;
    }

    static NormalizedNode<?, ?> create(final SchemaPath rpcName, final DataObject data,
            final BindingNormalizedNodeCodecRegistry codec) {
        return new LazySerializedContainerNode(rpcName.getLastComponent(), data, codec);
    }

    static NormalizedNode<?, ?> withContextRef(final SchemaPath rpcName, final DataObject data,
            final LeafNode<?> contextRef, final BindingNormalizedNodeCodecRegistry codec) {
        return new WithContextRef(rpcName.getLastComponent(), data, contextRef, codec);
    }

    @Override
    public Map<QName, String> getAttributes() {
        return delegate().getAttributes();
    }

    private ContainerNode delegate() {
        if (domData == null) {
            domData = registry.toNormalizedNodeRpcData(bindingData);
            registry = null;
        }
        return domData;
    }

    @Override
    public final QName getNodeType() {
        return identifier.getNodeType();
    }

    @Override
    public final Collection<DataContainerChild<? extends PathArgument, ?>> getValue() {
        return delegate().getValue();
    }

    @Override
    public final NodeIdentifier getIdentifier() {
        return identifier;
    }

    @Override
    public Optional<DataContainerChild<? extends PathArgument, ?>> getChild(final PathArgument child) {
        return delegate().getChild(child);
    }

    @Override
    public final Object getAttributeValue(final QName name) {
        return delegate().getAttributeValue(name);
    }

    final DataObject bindingData() {
        return bindingData;
    }

    /**
     * Lazy Serialized Node with pre-cached serialized leaf holding routing information.
     *
     */
    private static final class WithContextRef extends LazySerializedContainerNode {

        private final LeafNode<?> contextRef;

        protected WithContextRef(final QName identifier, final DataObject binding, final LeafNode<?> contextRef,
                final BindingNormalizedNodeCodecRegistry registry) {
            super(identifier, binding, registry);
            this.contextRef = contextRef;
        }

        @Override
        public Optional<DataContainerChild<? extends PathArgument, ?>> getChild(final PathArgument child) {
            /*
             * Use precached value of routing field and do not run full serialization if we are
             * accessing it.
             */
            if (contextRef.getIdentifier().equals(child)) {
                return Optional.<DataContainerChild<? extends PathArgument, ?>>of(contextRef);
            }
            return super.getChild(child);
        }
    }

}
