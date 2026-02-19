/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry.gossip;

import com.google.common.collect.ImmutableMap;
import java.io.Serializable;
import java.util.Map;
import org.apache.pekko.actor.Address;

final class GossipStatus implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<Address, Long> versions;
    private final Address from;

    GossipStatus(final Address from, final Map<Address, Long> versions) {
        this.versions = ImmutableMap.copyOf(versions);
        this.from = from;
    }

    Address from() {
        return from;
    }

    Map<Address, Long> versions() {
        return versions;
    }
}
