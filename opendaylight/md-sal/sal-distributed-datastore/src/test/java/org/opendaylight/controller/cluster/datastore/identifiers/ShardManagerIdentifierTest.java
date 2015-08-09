/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.identifiers;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

    public class ShardManagerIdentifierTest {

    @Test
    public void testIdentifier(){
        assertEquals("shardmanager-operational", ShardManagerIdentifier.builder().type("operational").build().toString());
    }

}
