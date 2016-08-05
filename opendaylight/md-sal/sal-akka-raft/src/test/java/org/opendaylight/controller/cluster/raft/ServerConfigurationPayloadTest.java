/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.persisted.ServerConfigurationPayload;
import org.opendaylight.controller.cluster.raft.persisted.ServerInfo;

/**
 * Unit tests for ServerConfigurationPayload.
 *
 * @author Thomas Pantelis
 */
public class ServerConfigurationPayloadTest {

    @Test
    public void testSerialization() {
        ServerConfigurationPayload expected = new ServerConfigurationPayload(Arrays.asList(new ServerInfo("1", true),
                new ServerInfo("2", false)));
        ServerConfigurationPayload cloned = (ServerConfigurationPayload) SerializationUtils.clone(expected);

        assertEquals("getServerConfig", expected.getServerConfig(), cloned.getServerConfig());
    }

    @Test
    public void testSize() {
        ServerConfigurationPayload expected = new ServerConfigurationPayload(Arrays.asList(new ServerInfo("1", true)));
        assertTrue(expected.size() > 0);
    }
}
