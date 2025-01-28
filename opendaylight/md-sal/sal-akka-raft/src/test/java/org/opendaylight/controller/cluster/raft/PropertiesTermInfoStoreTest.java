/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendaylight.controller.cluster.raft.spi.TermInfo;

/**
 * Unit tests for ElectionTermImpl.
 *
 * @author Thomas Pantelis
 */
class PropertiesTermInfoStoreTest {
    private Path stateFile;

    @BeforeEach
    void beforeEach() throws Exception {
        stateFile = Files.createTempFile(PropertiesTermInfoStoreTest.class.getName(), null);
    }

    @AfterEach
    void afterEach() throws Exception {
        Files.deleteIfExists(stateFile);
    }

    @Test
    void testUpdateAndPersist() throws Exception {
        final var first = new PropertiesTermInfoStore("test", stateFile);
        final var termInfo = new TermInfo(10, "member-1");
        first.storeAndSetTerm(termInfo);

        assertEquals(termInfo, first.currentTerm());

        assertTrue(Files.exists(stateFile));
        final var second = new PropertiesTermInfoStore("test", stateFile);
        assertEquals(termInfo, second.loadAndSetTerm());
        assertEquals(termInfo, second.currentTerm());
    }
}
