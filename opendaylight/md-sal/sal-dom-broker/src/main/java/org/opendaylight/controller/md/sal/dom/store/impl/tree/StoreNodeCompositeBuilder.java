/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl.tree;

import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.NormalizedNodeContainerBuilder;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedLong;

/**
 *
 * Helper builder
 *
 *
 */
@SuppressWarnings("rawtypes")
public class StoreNodeCompositeBuilder {

    private final StoreMetadataNode.Builder metadata;

    private final NormalizedNodeContainerBuilder data;

    private StoreNodeCompositeBuilder(final NormalizedNodeContainerBuilder nodeBuilder) {
        this.metadata = StoreMetadataNode.builder();
        this.data = Preconditions.checkNotNull(nodeBuilder);
    }

    public StoreNodeCompositeBuilder(NormalizedNodeContainerBuilder nodeBuilder, StoreMetadataNode currentMeta) {
        this.metadata = StoreMetadataNode.builder(currentMeta);
        this.data = Preconditions.checkNotNull(nodeBuilder);
    }

    @SuppressWarnings("unchecked")
    public StoreNodeCompositeBuilder add(final StoreMetadataNode node) {
        metadata.add(node);
        data.addChild(node.getData());
        return this;
    }

    @SuppressWarnings("unchecked")
    public StoreNodeCompositeBuilder remove(PathArgument id) {
        metadata.remove(id);
        data.removeChild(id);
        return this;
    }

    public StoreMetadataNode build() {
        return metadata.setData(data.build()).build();
    }

    public static StoreNodeCompositeBuilder from(final NormalizedNodeContainerBuilder nodeBuilder) {
        return new StoreNodeCompositeBuilder(nodeBuilder);
    }

    public static StoreNodeCompositeBuilder from(final NormalizedNodeContainerBuilder nodeBuilder, StoreMetadataNode currentMeta) {
        return new StoreNodeCompositeBuilder(nodeBuilder, currentMeta);
    }

    @SuppressWarnings("unchecked")
    public StoreNodeCompositeBuilder setIdentifier(final PathArgument identifier) {
        data.withNodeIdentifier(identifier);
        return this;
    }

    public StoreNodeCompositeBuilder setNodeVersion(final UnsignedLong nodeVersion) {
        metadata.setNodeVersion(nodeVersion);
        return this;
    }

    public StoreNodeCompositeBuilder setSubtreeVersion(final UnsignedLong updatedSubtreeVersion) {
        metadata.setSubtreeVersion(updatedSubtreeVersion);
        return this;
    }
}
