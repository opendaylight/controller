/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl;

import org.opendaylight.controller.md.sal.dom.store.impl.tree.StoreMetadataNode;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.TreeNodeUtils;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import com.google.common.base.Optional;
import com.google.common.primitives.UnsignedLong;

class DataAndMetadataSnapshot {

    private final StoreMetadataNode metadataTree;
    private final Optional<SchemaContext> schemaContext;

    private DataAndMetadataSnapshot(final StoreMetadataNode metadataTree, final Optional<SchemaContext> schemaCtx) {
        this.metadataTree = metadataTree;
        this.schemaContext = schemaCtx;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static DataAndMetadataSnapshot createEmpty() {
        return createEmpty(new NodeIdentifier(SchemaContext.NAME));
    }


    public static DataAndMetadataSnapshot createEmpty(final NodeIdentifier rootNode) {
        NormalizedNode<?, ?> data = Builders.containerBuilder().withNodeIdentifier(rootNode).build();
        StoreMetadataNode metadata = StoreMetadataNode.builder()
                .setNodeVersion(UnsignedLong.ZERO)
                .setSubtreeVersion(UnsignedLong.ZERO)
                .setData(data)
                .build();
        return new DataAndMetadataSnapshot(metadata,Optional.<SchemaContext>absent());
    }

    public static DataAndMetadataSnapshot createEmpty(final SchemaContext ctx) {
        NodeIdentifier rootNodeIdentifier = new NodeIdentifier(ctx.getQName());
        NormalizedNode<?, ?> data = Builders.containerBuilder().withNodeIdentifier(rootNodeIdentifier).build();
        StoreMetadataNode metadata = StoreMetadataNode.builder()
                .setData(data)
                .setNodeVersion(UnsignedLong.ZERO)
                .setSubtreeVersion(UnsignedLong.ZERO)
                .build();
        return new DataAndMetadataSnapshot(metadata, Optional.of(ctx));
    }

    public Optional<SchemaContext> getSchemaContext() {
        return schemaContext;
    }

    public NormalizedNode<?, ?> getDataTree() {
        return metadataTree.getData();
    }

    public StoreMetadataNode getMetadataTree() {
        return metadataTree;
    }

    public Optional<StoreMetadataNode> read(final InstanceIdentifier path) {
        return TreeNodeUtils.findNode(metadataTree, path);
    }

    public static class Builder {
        private NormalizedNode<?, ?> dataTree;
        private StoreMetadataNode metadataTree;
        private SchemaContext schemaContext;

        public NormalizedNode<?, ?> getDataTree() {
            return dataTree;
        }

        public Builder setMetadataTree(final StoreMetadataNode metadataTree) {
            this.metadataTree = metadataTree;
            return this;
        }

        public Builder setSchemaContext(final SchemaContext schemaContext) {
            this.schemaContext = schemaContext;
            return this;
        }

        public DataAndMetadataSnapshot build() {
            return new DataAndMetadataSnapshot(metadataTree, Optional.fromNullable(schemaContext));
        }

    }
}
