/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Optional;
import org.opendaylight.controller.cluster.datastore.utils.PruningDataTreeModification;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class ShardDataTreeSnapshot implements DataTreeSnapshot {

    private final DataTreeSnapshot dataTreeSnapshot;
    private final SchemaContext schemaContext;

    public ShardDataTreeSnapshot(DataTreeSnapshot dataTreeSnapshot, SchemaContext schemaContext) {
        this.dataTreeSnapshot = dataTreeSnapshot;
        this.schemaContext = schemaContext;
    }

    @Override
    public Optional<NormalizedNode<?, ?>> readNode(YangInstanceIdentifier yangInstanceIdentifier) {
        return this.dataTreeSnapshot.readNode(yangInstanceIdentifier);
    }

    @Override
    public DataTreeModification newModification() {
        return new PruningDataTreeModification(this.dataTreeSnapshot.newModification(), this.schemaContext);
    }


}
