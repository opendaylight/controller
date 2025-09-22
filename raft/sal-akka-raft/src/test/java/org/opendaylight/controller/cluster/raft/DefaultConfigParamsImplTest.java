/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opendaylight.controller.cluster.raft.policy.TestRaftPolicy;
import org.opendaylight.raft.spi.DefaultRaftPolicy;
import org.opendaylight.raft.spi.RaftPolicy;

public class DefaultConfigParamsImplTest {

    @Test
    public void testGetRaftPolicyWithDefault() {
        DefaultConfigParamsImpl params = new DefaultConfigParamsImpl();

        assertEquals("Default instance", DefaultRaftPolicy.INSTANCE, params.getRaftPolicy());
    }

    @Test
    public void testGetRaftPolicyInvalidClassName() {
        DefaultConfigParamsImpl params = new DefaultConfigParamsImpl();
        params.setCustomRaftPolicyImplementationClass("foobar");

        assertEquals("Default instance", DefaultRaftPolicy.INSTANCE, params.getRaftPolicy());
    }

    @Test
    public void testGetRaftPolicyValidClassNameButInvalidType() {
        DefaultConfigParamsImpl params = new DefaultConfigParamsImpl();
        params.setCustomRaftPolicyImplementationClass("java.lang.String");

        assertEquals("Default instance", DefaultRaftPolicy.INSTANCE, params.getRaftPolicy());
    }

    @Test
    public void testGetRaftPolicyValidClass() {
        DefaultConfigParamsImpl params1 = new DefaultConfigParamsImpl();
        params1.setCustomRaftPolicyImplementationClass(
                "org.opendaylight.controller.cluster.raft.policy.TestRaftPolicy");
        RaftPolicy behavior1 = params1.getRaftPolicy();

        assertEquals("TestCustomBehavior", TestRaftPolicy.class, behavior1.getClass());
        assertEquals("Same instance returned", behavior1, params1.getRaftPolicy());

        DefaultConfigParamsImpl params2 = new DefaultConfigParamsImpl();
        RaftPolicy behavior2 = params2.getRaftPolicy();
        params1.setCustomRaftPolicyImplementationClass(
                "org.opendaylight.controller.cluster.raft.policy.TestRaftPolicy");

        assertEquals("Default instance", DefaultRaftPolicy.INSTANCE, behavior2);
        assertEquals("Default instance", DefaultRaftPolicy.INSTANCE, params2.getRaftPolicy());

    }


}
