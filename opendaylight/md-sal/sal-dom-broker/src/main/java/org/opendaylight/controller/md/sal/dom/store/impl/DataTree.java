/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.opendaylight.controller.md.sal.dom.store.impl.tree.StoreMetadataNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizedNodeUtils;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

/**
 * Read-only snapshot of the data tree.
 */
final class DataTree {
    public static final class Snapshot {
        private final SchemaContext schemaContext;
        private final StoreMetadataNode rootNode;

        @VisibleForTesting
        Snapshot(final SchemaContext schemaContext, final StoreMetadataNode rootNode) {
            this.schemaContext = Preconditions.checkNotNull(schemaContext);
            this.rootNode = Preconditions.checkNotNull(rootNode);
        }

        public SchemaContext getSchemaContext() {
            return schemaContext;
        }

        public Optional<NormalizedNode<?, ?>> readNode(final InstanceIdentifier path) {
            return NormalizedNodeUtils.findNode(rootNode.getData(), path);
        }

        // FIXME: this is a leak of information
        @Deprecated
        StoreMetadataNode getRootNode() {
            return rootNode;
        }

        @Override
        public String toString() {
            return rootNode.getSubtreeVersion().toString();
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(DataTree.class);
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock(true);
    private StoreMetadataNode rootNode;
    private SchemaContext currentSchemaContext;

    private DataTree(StoreMetadataNode rootNode, final SchemaContext schemaContext) {
        this.rootNode = Preconditions.checkNotNull(rootNode);
        this.currentSchemaContext = schemaContext;
    }

    public synchronized void setSchemaContext(final SchemaContext newSchemaContext) {
        Preconditions.checkNotNull(newSchemaContext);

        LOG.info("Attepting to install schema context {}", newSchemaContext);

        /*
         * FIXME: we should walk the schema contexts, both current and new and see
         *        whether they are compatible here. Reject incompatible changes.
         */

        // Ready to change the context now, make sure no operations are running
        rwLock.writeLock().lock();
        try {
            this.currentSchemaContext = newSchemaContext;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public static DataTree create(final SchemaContext schemaContext) {
        final NodeIdentifier root = new NodeIdentifier(SchemaContext.NAME);
        final NormalizedNode<?, ?> data = Builders.containerBuilder().withNodeIdentifier(root).build();

        return new DataTree(StoreMetadataNode.createEmpty(data), schemaContext);
    }

    public Snapshot takeSnapshot() {
        rwLock.readLock().lock();

        try {
            return new Snapshot(currentSchemaContext, rootNode);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public void commitSnapshot(Snapshot currentSnapshot, StoreMetadataNode newDataTree) {
        // Ready to change the context now, make sure no operations are running
        rwLock.writeLock().lock();
        try {
            Preconditions.checkState(currentSnapshot.rootNode == rootNode,
                    String.format("Store snapshot %s and transaction snapshot %s differ.",
                            rootNode, currentSnapshot.rootNode));

            this.rootNode = newDataTree;
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
