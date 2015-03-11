/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
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

    LazySerializedContainerNode(QName identifier, DataObject binding,
            BindingNormalizedNodeCodecRegistry registry) {
        this.identifier = new NodeIdentifier(identifier);
        this.bindingData = binding;
        this.registry = registry;
        this.domData = null;
    }

    @Override
    public Map<QName, String> getAttributes() {
        return delegate().getAttributes();
    }

    private ContainerNode delegate() {
        if(domData == null) {
            domData = registry.toNormalizedNodeRpcData(bindingData);
            registry = null;
        }
        return domData;
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
        return identifier;
    }

    @Override
    public Optional<DataContainerChild<? extends PathArgument, ?>> getChild(PathArgument child) {
        return delegate().getChild(child);
    }

    @Override
    public Object getAttributeValue(QName name) {
        return delegate().getAttributeValue(name);
    }

    public DataObject bindingData() {
        return bindingData;
    }

}
