/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opendaylight.raft.api.TermInfo;

/**
 * Unit tests for ElectionTermImpl.
 *
 * @author Thomas Pantelis
 */
class PropertiesTermInfoStoreTest {
    @TempDir
    private Path directory;

    @Test
    void testUpdateAndPersist() throws Exception {
        final var first = new PropertiesTermInfoStore("test", directory);
        final var termInfo = new TermInfo(10, "member-1");
        first.storeAndSetTerm(termInfo);

        assertEquals(termInfo, first.currentTerm());

        assertThat(directory.resolve("TermInfo.properties")).isRegularFile();

        final var second = new PropertiesTermInfoStore("test", directory);
        assertEquals(termInfo, second.loadAndSetTerm());
        assertEquals(termInfo, second.currentTerm());
    }
}
