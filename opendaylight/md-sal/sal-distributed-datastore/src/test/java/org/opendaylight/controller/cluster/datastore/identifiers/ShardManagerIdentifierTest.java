/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.identifiers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opendaylight.controller.cluster.datastore.shardmanager.ShardManagerIdentifier;

class ShardManagerIdentifierTest {
    @Test
    void testIdentifier() {
        final var id = new ShardManagerIdentifier("operational");
        assertEquals("shardmanager-operational", id.toActorName());
        assertEquals("shardmanager-operational", id.toString());
    }
}
