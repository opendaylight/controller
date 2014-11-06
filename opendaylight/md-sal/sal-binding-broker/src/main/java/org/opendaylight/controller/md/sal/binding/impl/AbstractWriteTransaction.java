/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizationException;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizationOperation;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.CheckedFuture;

/**
 *
 * Abstract Base Transaction for transactions which are backed by
 * {@link DOMDataWriteTransaction}
 */
public abstract class AbstractWriteTransaction<T extends DOMDataWriteTransaction> extends
        AbstractForwardedTransaction<T> {

    protected AbstractWriteTransaction(final T delegate, final BindingToNormalizedNodeCodec codec) {
        super(delegate, codec);
    }

    public final <U extends DataObject> void put(final LogicalDatastoreType store,
            final InstanceIdentifier<U> path, final U data, final boolean createParents) {
        Preconditions.checkArgument(!path.isWildcarded(), "Cannot put data into wildcarded path %s", path);

        final Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> normalized = getCodec().toNormalizedNode(path, data);
        if (createParents) {
            ensureParentsByMerge(store, normalized.getKey(), path);
        } else {
            ensureListParentIfNeeded(store,path,normalized);
        }

        getDelegate().put(store, normalized.getKey(), normalized.getValue());
    }

    public final <U extends DataObject> void merge(final LogicalDatastoreType store,
            final InstanceIdentifier<U> path, final U data,final boolean createParents) {
        Preconditions.checkArgument(!path.isWildcarded(), "Cannot merge data into wildcarded path %s", path);

        final Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> normalized = getCodec().toNormalizedNode(path, data);
        if (createParents) {
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
            final Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> normalized) {
        if (Identifiable.class.isAssignableFrom(path.getTargetType())) {
            final YangInstanceIdentifier parentMapPath = getParent(normalized.getKey()).get();
            final NormalizedNode<?, ?> emptyParent = getCodec().getDefaultNodeFor(parentMapPath);
            getDelegate().merge(store, parentMapPath, emptyParent);
        }
    }

    // FIXME (should be probaly part of InstanceIdentifier)
    protected static Optional<YangInstanceIdentifier> getParent(
            final YangInstanceIdentifier child) {

        final Iterable<PathArgument> mapEntryItemPath = child.getPathArguments();
        final int parentPathSize = Iterables.size(mapEntryItemPath) - 1;
        if (parentPathSize > 1) {
            return Optional.of(YangInstanceIdentifier.create(Iterables.limit(mapEntryItemPath,  parentPathSize)));
        } else if(parentPathSize == 0) {
            return Optional.of(YangInstanceIdentifier.create(Collections.<PathArgument>emptyList()));
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
    protected final void ensureParentsByMerge(final LogicalDatastoreType store,
            final YangInstanceIdentifier normalizedPath, final InstanceIdentifier<?> path) {
        final List<PathArgument> currentArguments = new ArrayList<>();
        DataNormalizationOperation<?> currentOp = getCodec().getDataNormalizer().getRootOperation();
        final Iterator<PathArgument> iterator = normalizedPath.getPathArguments().iterator();
        while (iterator.hasNext()) {
            final PathArgument currentArg = iterator.next();
            // We process up to last element, last element is inserted and provided by user
            if (iterator.hasNext()) {
                try {
                    currentOp = currentOp.getChild(currentArg);
                } catch (final DataNormalizationException e) {
                    throw new IllegalArgumentException(String.format("Invalid child encountered in path %s", path), e);
                }
                currentArguments.add(currentArg);
                final YangInstanceIdentifier currentPath = YangInstanceIdentifier.create(currentArguments);

                getDelegate().merge(store, currentPath, currentOp.createDefault(currentArg));
            }
        }

    }

    protected final void doDelete(final LogicalDatastoreType store,
            final InstanceIdentifier<?> path) {
        Preconditions.checkArgument(!path.isWildcarded(), "Cannot delete wildcarded path %s", path);

        final YangInstanceIdentifier normalized = getCodec().toNormalized(path);
        getDelegate().delete(store, normalized);
    }

    protected final CheckedFuture<Void,TransactionCommitFailedException> doSubmit() {
        return getDelegate().submit();
    }

    protected final boolean doCancel() {
        return getDelegate().cancel();
    }

}
