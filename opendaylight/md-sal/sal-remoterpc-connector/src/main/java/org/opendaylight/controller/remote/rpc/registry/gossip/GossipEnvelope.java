/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry.gossip;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableMap;
import java.io.Serializable;
import java.util.Map;
import org.apache.pekko.actor.Address;

final class GossipEnvelope implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<Address, Bucket<?>> buckets;
    private final Address from;
    private final Address to;

    GossipEnvelope(final Address from, final Address to, final Map<Address, ? extends Bucket<?>> buckets) {
        this.to = requireNonNull(to);
        this.buckets = ImmutableMap.copyOf(buckets);
        this.from = from;
    }

    Map<Address, Bucket<?>> buckets() {
        return buckets;
    }

    Address from() {
        return from;
    }

    Address to() {
        return to;
    }
}
