/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.opendaylight.raft.spi.WellKnownRaftPolicy;

class DefaultConfigParamsImplTest {
    private final DefaultConfigParamsImpl params = new DefaultConfigParamsImpl();

    @Test
    void testGetRaftPolicyWithDefault() {
       assertSame(WellKnownRaftPolicy.NORMAL, params.getRaftPolicy());
    }

    @Test
    void testGetRaftPolicyValidClass() {
        params.setRaftPolicy(WellKnownRaftPolicy.DISABLE_ELECTIONS);
        assertSame(WellKnownRaftPolicy.DISABLE_ELECTIONS, params.getRaftPolicy());
    }
}
