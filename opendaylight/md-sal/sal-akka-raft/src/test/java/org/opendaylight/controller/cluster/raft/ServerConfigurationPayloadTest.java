/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.assertEquals;
import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;

/**
 * Unit tests for ServerConfigurationPayload.
 *
 * @author Thomas Pantelis
 */
@Deprecated
public class ServerConfigurationPayloadTest {

    @Test
    public void testSerialization() {
        ServerConfigurationPayload expected = new ServerConfigurationPayload(Arrays.asList(
                new ServerConfigurationPayload.ServerInfo("1", true),
                new ServerConfigurationPayload.ServerInfo("2", false)));
        org.opendaylight.controller.cluster.raft.persisted.ServerConfigurationPayload cloned =
                (org.opendaylight.controller.cluster.raft.persisted.ServerConfigurationPayload) SerializationUtils.clone(expected);

        assertEquals("getServerConfig", ImmutableSet.of(
                new org.opendaylight.controller.cluster.raft.persisted.ServerInfo("1", true),
                new org.opendaylight.controller.cluster.raft.persisted.ServerInfo("2", false)),
                ImmutableSet.copyOf(cloned.getServerConfig()));
        assertEquals("isMigrated", true, cloned.isMigrated());
    }
}
