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

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.CheckedFuture;

/**
 *
 * Abstract Base Transaction for transactions which are backed by
 * {@link DOMDataWriteTransaction}
 */
public abstract class AbstractWriteTransaction<T extends DOMDataWriteTransaction> extends
        AbstractForwardedTransaction<T> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractWriteTransaction.class);

    protected AbstractWriteTransaction(final T delegate,
            final BindingToNormalizedNodeCodec codec) {
        super(delegate, codec);
    }


    public final <U extends DataObject> void put(final LogicalDatastoreType store,
            final InstanceIdentifier<U> path, final U data, final boolean createParents) {
       final Entry<org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier, NormalizedNode<?, ?>> normalized = getCodec()
                .toNormalizedNode(path, data);
        if(createParents) {
            ensureParentsByMerge(store, normalized.getKey(), path);
        } else {
            ensureListParentIfNeeded(store,path,normalized);
        }
        getDelegate().put(store, normalized.getKey(), normalized.getValue());
    }


    public final <U extends DataObject> void merge(final LogicalDatastoreType store,
            final InstanceIdentifier<U> path, final U data,final boolean createParents) {

        final Entry<org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier, NormalizedNode<?, ?>> normalized = getCodec()
                .toNormalizedNode(path, data);

        if(createParents) {
            ensureParentsByMerge(store, normalized.getKey(), path);
        } else {
            ensureListParentIfNeeded(store,path,normalized);
        }

        getDelegate().merge(store, normalized.getKey(), normalized.getValue());
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
            final Entry<org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier, NormalizedNode<?, ?>> normalized) {
        if(Identifiable.class.isAssignableFrom(path.getTargetType())) {
            org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier parentMapPath = getParent(normalized.getKey()).get();
            NormalizedNode<?, ?> emptyParent = getCodec().getDefaultNodeFor(parentMapPath);
            getDelegate().merge(store, parentMapPath, emptyParent);
        }

    }

    // FIXME (should be probaly part of InstanceIdentifier)
    protected static Optional<org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier> getParent(
            final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier child) {

        Iterable<PathArgument> mapEntryItemPath = child.getPathArguments();
        int parentPathSize = Iterables.size(mapEntryItemPath) - 1;
        if(parentPathSize > 1) {
            return Optional.of(org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.create(Iterables.limit(mapEntryItemPath,  parentPathSize)));
        } else if(parentPathSize == 0) {
            return Optional.of(org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.create(Collections.<PathArgument>emptyList()));
        } else {
            return Optional.absent();
        }
    }

    /**
     * Subclasses of this class are required to implement creation of parent
     * nodes based on behaviour of their underlying transaction.
     *
     * @param store
     * @param key
     * @param path
     */
    protected abstract void ensureParentsByMerge(LogicalDatastoreType store,
            org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier key, InstanceIdentifier<?> path);

    protected final void doDelete(final LogicalDatastoreType store,
            final InstanceIdentifier<?> path) {
        final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier normalized = getCodec().toNormalized(path);
        getDelegate().delete(store, normalized);
    }

    protected final CheckedFuture<Void,TransactionCommitFailedException> doSubmit() {
        return getDelegate().submit();
    }

    protected final boolean doCancel() {
        return getDelegate().cancel();
    }

}
