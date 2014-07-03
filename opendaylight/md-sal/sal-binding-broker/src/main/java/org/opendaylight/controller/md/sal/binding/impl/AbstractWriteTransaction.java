/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import java.util.Collections;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.ListenableFuture;

/**
 *
 * Abstract Base Transaction for transactions which are backed by
 * {@link DOMDataWriteTransaction}
 */
public class AbstractWriteTransaction<T extends DOMDataWriteTransaction> extends
        AbstractForwardedTransaction<T> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractWriteTransaction.class);

    protected AbstractWriteTransaction(final T delegate,
            final BindingToNormalizedNodeCodec codec) {
        super(delegate, codec);
    }

    protected final void doPut(final LogicalDatastoreType store,
            final InstanceIdentifier<?> path, final DataObject data) {
       final Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, NormalizedNode<?, ?>> normalized = getCodec()
                .toNormalizedNode(path, data);
        ensureListParentIfNeeded(store,path,normalized);
        getDelegate().put(store, normalized.getKey(), normalized.getValue());
    }


    /**
     *
     * Ensures list parent if item is list, otherwise noop.
     *
     * <p>
     * One of properties of binding specification is that it is imposible
     * to represent list as a whole and thus it is impossible to write
     * empty variation of MapNode without creating parent node, with
     * empty list.
     *
     * <p>
     * This actually makes writes such as
     * <pre>
     * put("Nodes", new NodesBuilder().build());
     * put("Nodes/Node[key]", new NodeBuilder().setKey("key").build());
     * </pre>
     * To result in three DOM operations:
     * <pre>
     * put("/nodes",domNodes);
     * merge("/nodes/node",domNodeList);
     * put("/nodes/node/node[key]",domNode);
     * </pre>
     *
     *
     * In order to allow that to be inserted if necessary, if we know
     * item is list item, we will try to merge empty MapNode or OrderedNodeMap
     * to ensure list exists.
     *
     * @param store Data Store type
     * @param path Path to data (Binding Aware)
     * @param normalized Normalized version of data to be written
     */
    private void ensureListParentIfNeeded(final LogicalDatastoreType store, final InstanceIdentifier<?> path,
            final Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, NormalizedNode<?, ?>> normalized) {
        if(Identifiable.class.isAssignableFrom(path.getTargetType())) {
            org.opendaylight.yangtools.yang.data.api.InstanceIdentifier parentMapPath = getParent(normalized.getKey()).get();
            NormalizedNode<?, ?> emptyParent = getCodec().getDefaultNodeFor(parentMapPath);
            getDelegate().merge(store, parentMapPath, emptyParent);
        }

    }

    // FIXME (should be probaly part of InstanceIdentifier)
    protected static Optional<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier> getParent(
            final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier child) {

        Iterable<PathArgument> mapEntryItemPath = child.getPathArguments();
        int parentPathSize = Iterables.size(mapEntryItemPath) - 1;
        if(parentPathSize > 1) {
            return Optional.of(org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.create(Iterables.limit(mapEntryItemPath,  parentPathSize)));
        } else if(parentPathSize == 0) {
            return Optional.of(org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.create(Collections.<PathArgument>emptyList()));
        } else {
            return Optional.absent();
        }
    }

    protected final void doMerge(final LogicalDatastoreType store,
            final InstanceIdentifier<?> path, final DataObject data) {

        final Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, NormalizedNode<?, ?>> normalized = getCodec()
                .toNormalizedNode(path, data);
        ensureListParentIfNeeded(store,path,normalized);
        getDelegate().merge(store, normalized.getKey(), normalized.getValue());
    }

    protected final void doDelete(final LogicalDatastoreType store,
            final InstanceIdentifier<?> path) {
        final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier normalized = getCodec().toNormalized(path);
        getDelegate().delete(store, normalized);
    }

    protected final ListenableFuture<RpcResult<TransactionStatus>> doCommit() {
        return getDelegate().commit();
    }

    protected final boolean doCancel() {
        return getDelegate().cancel();
    }

}
