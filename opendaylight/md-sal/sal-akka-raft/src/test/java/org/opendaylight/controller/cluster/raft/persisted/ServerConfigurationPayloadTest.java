/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;

/**
 * Unit tests for ServerConfigurationPayload.
 *
 * @author Thomas Pantelis
 */
public class ServerConfigurationPayloadTest {
    @Test
    public void testSerialization() {
        final var expected = new ServerConfigurationPayload(List.of(new ServerInfo("1", true),
            new ServerInfo("2", false)));

        final var bytes = SerializationUtils.serialize(expected);
        assertEquals(125, bytes.length);
        final var cloned = (ServerConfigurationPayload) SerializationUtils.deserialize(bytes);

        assertEquals("getServerConfig", expected.getServerConfig(), cloned.getServerConfig());
    }

    @Test
    public void testSize() {
        final var expected = new ServerConfigurationPayload(List.of(new ServerInfo("1", true)));
        assertTrue(expected.size() > 0);
    }
}
