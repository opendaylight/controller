/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map.Entry;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.binding.data.codec.api.BindingCodecTreeNode;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;

class LazyDataTreeModification implements DataTreeModification {

    private final DataTreeIdentifier path;
    private final BindingCodecTreeNode<?> rootCtx;
    private final DataTreeCandidate domChange;

    LazyDataTreeModification(final LogicalDatastoreType datastoreType, final InstanceIdentifier<?> path, final BindingCodecTreeNode<?> codec, final DataTreeCandidate domChange) {
        this.path = new DataTreeIdentifier(datastoreType,path);
        this.rootCtx = Preconditions.checkNotNull(codec);
        this.domChange = Preconditions.checkNotNull(domChange);
    }

    @Override
    public DataObjectModification<? extends DataObject> getRootNode() {
        return LazyDataObjectModification.create(rootCtx,domChange.getRootNode());
    }

    @Override
    public DataTreeIdentifier getRootPath() {
        return path;
    }

    static DataTreeModification create(final BindingToNormalizedNodeCodec codec, final DataTreeCandidate domChange, final LogicalDatastoreType datastoreType) {
        final Entry<InstanceIdentifier<?>, BindingCodecTreeNode<?>> codecCtx = codec.getSubtreeCodec(domChange.getRootPath());
        return new LazyDataTreeModification(datastoreType,codecCtx.getKey(),codecCtx.getValue(),domChange);
    }

    static Collection<DataTreeModification> from(final BindingToNormalizedNodeCodec codec,
            final Collection<DataTreeCandidate> domChanges, final LogicalDatastoreType datastoreType) {
        final ArrayList<DataTreeModification> result = new ArrayList<>(domChanges.size());
        for(final DataTreeCandidate domChange : domChanges) {
            result.add(create(codec, domChange, datastoreType));
        }
        return result;
    }

}
