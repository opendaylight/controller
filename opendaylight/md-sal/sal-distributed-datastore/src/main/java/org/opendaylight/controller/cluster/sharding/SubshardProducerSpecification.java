/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.sharding;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.spi.shard.DOMDataTreeShardProducer;

public class SubshardProducerSpecification {
    private final Collection<DOMDataTreeIdentifier> prefixes = new ArrayList<>(1);
    private final ChildShardContext shard;

    SubshardProducerSpecification(final ChildShardContext subshard) {
        this.shard = Preconditions.checkNotNull(subshard);
    }

    void addPrefix(final DOMDataTreeIdentifier prefix) {
        prefixes.add(prefix);
    }

    DOMDataTreeShardProducer createProducer() {
        return shard.getShard().createProducer(prefixes);
    }

    public DOMDataTreeIdentifier getPrefix() {
        return shard.getPrefix();
    }
}
