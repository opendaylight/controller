/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.sharding;

import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.spi.shard.AbstractShardModificationFactoryBuilder;

/**
 * Builder for {@link DistributedShardModificationFactory}.
 */
@Deprecated(forRemoval = true)
public class DistributedShardModificationFactoryBuilder
        extends AbstractShardModificationFactoryBuilder<DistributedShardModificationFactory> {


    public DistributedShardModificationFactoryBuilder(final DOMDataTreeIdentifier root) {
        super(root);
    }

    @Override
    public DistributedShardModificationFactory build() {
        return new DistributedShardModificationFactory(root, buildChildren(), childShards);
    }
}
