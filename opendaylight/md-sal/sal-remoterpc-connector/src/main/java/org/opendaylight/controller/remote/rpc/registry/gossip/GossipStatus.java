/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry.gossip;

import akka.actor.Address;
import java.util.Map;

final class GossipStatus extends BucketStoreMessages.ContainsBucketVersions {
    private static final long serialVersionUID = -593037395143883265L;

    private final Address from;

    GossipStatus(final Address from, final Map<Address, Long> versions) {
        super(versions);
        this.from = from;
    }

    Address from() {
        return from;
    }
}