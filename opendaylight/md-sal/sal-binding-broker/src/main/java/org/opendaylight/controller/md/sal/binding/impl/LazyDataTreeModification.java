/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.binding.data.codec.api.BindingCodecTreeNode;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;

/**
 * Lazily translated {@link DataTreeModification} based on {@link DataTreeCandidate}.
 *
 * {@link DataTreeModification} represents Data tree change event,
 * but whole tree is not translated or resolved eagerly, but only child nodes
 * which are directly accessed by user of data object modification.
 *
 */
class LazyDataTreeModification<T extends DataObject> implements DataTreeModification<T> {

    private final DataTreeIdentifier<T> path;
    private final DataObjectModification<T> rootNode;

    LazyDataTreeModification(final LogicalDatastoreType datastoreType, final InstanceIdentifier<T> path, final BindingCodecTreeNode<T> codec, final DataTreeCandidate domChange) {
        this.path = new DataTreeIdentifier<>(datastoreType, path);
        this.rootNode = LazyDataObjectModification.create(codec, domChange.getRootNode());
    }

    @Override
    public DataObjectModification<T> getRootNode() {
        return rootNode;
    }

    @Override
    public DataTreeIdentifier<T> getRootPath() {
        return path;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static <T extends DataObject> DataTreeModification<T> create(final BindingToNormalizedNodeCodec codec, final DataTreeCandidate domChange,
            final LogicalDatastoreType datastoreType) {
        final Entry<InstanceIdentifier<?>, BindingCodecTreeNode<?>> codecCtx =
                codec.getSubtreeCodec(domChange.getRootPath());
        return new LazyDataTreeModification(datastoreType, codecCtx.getKey(), codecCtx.getValue(), domChange);
    }

    static <T extends DataObject> Collection<DataTreeModification<T>> from(final BindingToNormalizedNodeCodec codec,
            final Collection<DataTreeCandidate> domChanges, final LogicalDatastoreType datastoreType) {
        final List<DataTreeModification<T>> result = new ArrayList<>(domChanges.size());
        for (final DataTreeCandidate domChange : domChanges) {
            result.add(LazyDataTreeModification.<T>create(codec, domChange, datastoreType));
        }
        return result;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{path = " + path + ", rootNode = " + rootNode + "}";
    }
}
