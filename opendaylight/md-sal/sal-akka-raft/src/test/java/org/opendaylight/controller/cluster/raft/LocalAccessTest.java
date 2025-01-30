/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * A test that has some {@code state directory} footprint expressed as {@link LocalAccess}.
 */
abstract class LocalAccessTest {
    // dedicated test dir
    private Path stateDir;

    protected LocalAccess localAccess;

    @BeforeEach
    void beforeEach() throws Exception {
        stateDir = Files.createTempDirectory(PropertiesTermInfoStoreTest.class.getName());
        localAccess = new LocalAccess("test", stateDir);
    }

    @AfterEach
    void afterEach() throws Exception {
        try (var paths = Files.walk(stateDir)) {
            paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
    }
}
