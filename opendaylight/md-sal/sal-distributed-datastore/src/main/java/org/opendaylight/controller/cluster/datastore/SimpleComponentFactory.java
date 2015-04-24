/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionFactory;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;

// FIXME: this should be integrated into DistributedDataStore
final class SimpleComponentFactory extends ComponentFactory {
    protected SimpleComponentFactory(ActorContext actorContext) {
        super(actorContext);
    }

    @Override
    protected DOMStoreTransactionFactory factoryForShard(final String shardName, final DataTree dataTree) {
        // TODO Auto-generated method stub
        return null;
    }
}
