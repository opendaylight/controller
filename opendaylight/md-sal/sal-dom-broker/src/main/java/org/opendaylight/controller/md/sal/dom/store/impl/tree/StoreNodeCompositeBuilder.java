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
        this.data = nodeBuilder;
    }

    @SuppressWarnings("unchecked")
    public StoreNodeCompositeBuilder add(final StoreMetadataNode node) {
        metadata.add(node);
        data.addChild(node.getData());
        return this;
    }


    public StoreMetadataNode build() {
        return metadata.setData(data.build()).build();
    }


    public static StoreNodeCompositeBuilder from(final NormalizedNodeContainerBuilder nodeBuilder) {
        return new StoreNodeCompositeBuilder(nodeBuilder);
    }

    public static StoreNodeCompositeBuilder from(final StoreMetadataNode previous, final NormalizedNodeContainerBuilder nodeBuilder) {

        return new StoreNodeCompositeBuilder(nodeBuilder);
    }

    public StoreNodeCompositeBuilder setIdentifier(final PathArgument identifier) {
        data.withNodeIdentifier(identifier);
        return this;
    }

}
