/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.sharding;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.databroker.actors.dds.DistributedDataStoreClient;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.store.inmemory.ShardDataModificationFactoryBuilder;

public class DistributedShardModificationFactoryBuilder
        extends ShardDataModificationFactoryBuilder<DistributedShardModificationFactory> {

    private DistributedDataStoreClient client;

    public DistributedShardModificationFactoryBuilder(final DOMDataTreeIdentifier root,
                                                      final DistributedDataStoreClient client) {
        super(root);
        this.client = Preconditions.checkNotNull(client);
    }

    @Override
    public DistributedShardModificationFactory build() {
        return new DistributedShardModificationFactory(root, buildChildren(), childShards);
    }
}
