/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry.gossip;

import akka.actor.Address;
import com.google.common.base.Preconditions;
import java.util.Map;

final class GossipEnvelope<T extends BucketData<T>> extends BucketStoreMessages.ContainsBuckets<T> {
    private static final long serialVersionUID = 8346634072582438818L;

    private final Address from;
    private final Address to;

    GossipEnvelope(final Address from, final Address to, final Map<Address, Bucket<T>> buckets) {
        super(buckets);
        Preconditions.checkArgument(to != null, "Recipient of message must not be null");
        this.to = to;
        this.from = from;
    }

    Address from() {
        return from;
    }

    Address to() {
        return to;
    }
}