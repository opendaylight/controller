/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.identifiers;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.concepts.MemberName;

public class ShardIdentifierTest {

    @Test
    public void testBasic(){
        ShardIdentifier id = ShardIdentifier.builder().memberName(MemberName.forName("member-1"))
            .shardName("inventory").type("config").build();

        assertEquals("member-1-shard-inventory-config", id.toString());
    }

    @Test
    public void testFromShardIdString(){
        String shardIdStr = "member-1-shard-inventory-config";

        ShardIdentifier id = ShardIdentifier.builder().fromShardIdString(shardIdStr).build();

        assertEquals("member-1", id.getMemberName().getName());
        assertEquals("inventory", id.getShardName());
        assertEquals("config", id.getType());
    }
}
