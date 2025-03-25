/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for ServerConfigurationPayload.
 *
 * @author Thomas Pantelis
 */
class ServerConfigurationPayloadTest {
    @Test
    void testSerialization() {
        final var expected = new ClusterConfig(new ServerInfo("1", true), new ServerInfo("2", false));

        final var bytes = SerializationUtils.serialize(expected);
        assertEquals(125, bytes.length);
        final var cloned = (ClusterConfig) SerializationUtils.deserialize(bytes);

        assertEquals(expected.serverInfo(), cloned.serverInfo());
    }

    @Test
    void testSize() {
        final var expected = new ClusterConfig(new ServerInfo("1", true));
        assertTrue(expected.size() > 0);
    }
}
