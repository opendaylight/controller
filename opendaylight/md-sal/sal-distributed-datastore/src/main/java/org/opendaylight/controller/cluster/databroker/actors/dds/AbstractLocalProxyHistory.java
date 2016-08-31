/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;

abstract class AbstractLocalProxyHistory extends AbstractProxyHistory {
    private final DataTree dataTree;

    AbstractLocalProxyHistory(final DistributedDataStoreClientBehavior client, final LocalHistoryIdentifier identifier,
        final DataTree dataTree) {
        super(client, identifier);
        this.dataTree = Preconditions.checkNotNull(dataTree);
    }

    final DataTreeSnapshot takeSnapshot() {
        return dataTree.takeSnapshot();
    }
}
