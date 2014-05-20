/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl.tree.data;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.opendaylight.controller.md.sal.dom.store.impl.tree.DataPreconditionFailedException;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.DataTree;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.DataTreeCandidate;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.DataTreeModification;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.StoreUtils;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

/**
 * Read-only snapshot of the data tree.
 */
final class InMemoryDataTree implements DataTree {
    private static final Logger LOG = LoggerFactory.getLogger(InMemoryDataTree.class);
    private static final InstanceIdentifier PUBLIC_ROOT_PATH = InstanceIdentifier.builder().build();

    private final ReadWriteLock rwLock = new ReentrantReadWriteLock(true);
    private ModificationApplyOperation applyOper = new AlwaysFailOperation();
    private SchemaContext currentSchemaContext;
    private StoreMetadataNode rootNode;

    public InMemoryDataTree(StoreMetadataNode rootNode, final SchemaContext schemaContext) {
        this.rootNode = Preconditions.checkNotNull(rootNode);

        if (schemaContext != null) {
            // Also sets applyOper
            setSchemaContext(schemaContext);
        }
    }

    @Override
    public synchronized void setSchemaContext(final SchemaContext newSchemaContext) {
        Preconditions.checkNotNull(newSchemaContext);

        LOG.info("Attepting to install schema context {}", newSchemaContext);

        /*
         * FIXME: we should walk the schema contexts, both current and new and see
         *        whether they are compatible here. Reject incompatible changes.
         */

        // Instantiate new apply operation, this still may fail
        final ModificationApplyOperation newApplyOper = SchemaAwareApplyOperation.from(newSchemaContext);

        // Ready to change the context now, make sure no operations are running
        rwLock.writeLock().lock();
        try {
            this.applyOper = newApplyOper;
            this.currentSchemaContext = newSchemaContext;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public InMemoryDataTreeSnapshot takeSnapshot() {
        rwLock.readLock().lock();
        try {
            return new InMemoryDataTreeSnapshot(currentSchemaContext, rootNode, applyOper);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public void validate(DataTreeModification modification) throws DataPreconditionFailedException {
        Preconditions.checkArgument(modification instanceof InMemoryDataTreeModification, "Invalid modification class %s", modification.getClass());

        final InMemoryDataTreeModification m = (InMemoryDataTreeModification)modification;
        m.getStrategy().checkApplicable(PUBLIC_ROOT_PATH, m.getRootModification(), Optional.of(rootNode));
    }

    @Override
    public synchronized DataTreeCandidate prepare(DataTreeModification modification) {
        Preconditions.checkArgument(modification instanceof InMemoryDataTreeModification, "Invalid modification class %s", modification.getClass());

        final InMemoryDataTreeModification m = (InMemoryDataTreeModification)modification;
        final NodeModification root = m.getRootModification();

        if (root.getModificationType() == ModificationType.UNMODIFIED) {
            return new NoopDataTreeCandidate(PUBLIC_ROOT_PATH, root);
        }

        rwLock.writeLock().lock();
        try {
            // FIXME: rootNode needs to be a read-write snapshot here...
            final Optional<StoreMetadataNode> newRoot = m.getStrategy().apply(m.getRootModification(), Optional.of(rootNode), StoreUtils.increase(rootNode.getSubtreeVersion()));
            Preconditions.checkState(newRoot.isPresent(), "Apply strategy failed to produce root node");
            return new InMemoryDataTreeCandidate(PUBLIC_ROOT_PATH, root, rootNode, newRoot.get());
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public synchronized void commit(DataTreeCandidate candidate) {
        if (candidate instanceof NoopDataTreeCandidate) {
            return;
        }

        Preconditions.checkArgument(candidate instanceof InMemoryDataTreeCandidate, "Invalid candidate class %s", candidate.getClass());
        final InMemoryDataTreeCandidate c = (InMemoryDataTreeCandidate)candidate;

        LOG.debug("Updating Store snapshot version: {} with version:{}", rootNode.getSubtreeVersion(), c.getAfterRoot().getSubtreeVersion());

        if (LOG.isTraceEnabled()) {
            LOG.trace("Data Tree is {}", StoreUtils.toStringTree(c.getAfterRoot().getData()));
        }

        // Ready to change the context now, make sure no operations are running
        rwLock.writeLock().lock();
        try {
            Preconditions.checkState(c.getBeforeRoot() == rootNode,
                    String.format("Store snapshot %s and transaction snapshot %s differ.", rootNode, c.getBeforeRoot()));
            this.rootNode = c.getAfterRoot();
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
