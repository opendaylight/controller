/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.persistence;

import com.typesafe.config.ConfigFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.io.FileUtils;
import org.apache.pekko.persistence.japi.snapshot.JavaSnapshotStoreSpec;
import org.junit.runner.RunWith;
import org.scalatestplus.junit.JUnitRunner;

/**
 * Tests the LocalSnapshotStore using akka's standard test suite for snapshot store plugins via SnapshotStoreSpec.
 * This class basically does the setup and tear down with SnapshotStoreSpec doing the rest. SnapshotStoreSpec uses
 * ScalaTest so needs to be run with scala's JUnitRunner.
 *
 * @author Thomas Pantelis
 */
@RunWith(JUnitRunner.class)
@Deprecated(since = "11.0.0", forRemoval = true)
public class LocalSnapshotStoreSpecTest extends JavaSnapshotStoreSpec {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    static final Path SNAPSHOT_DIR = Path.of("target", "snapshots");

    public LocalSnapshotStoreSpecTest() {
        super(ConfigFactory.load("LocalSnapshotStoreTest.conf"));
    }

    @Override
    public void afterAll() {
        super.afterAll();
        FileUtils.deleteQuietly(SNAPSHOT_DIR.toFile());
    }

    static void cleanSnapshotDir() {
        if (Files.exists(SNAPSHOT_DIR)) {
            try {
                FileUtils.cleanDirectory(SNAPSHOT_DIR.toFile());
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    static void createSnapshotDir() {
        try {
            Files.createDirectories(SNAPSHOT_DIR);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create " + SNAPSHOT_DIR, e);
        }
        cleanSnapshotDir();
    }
}
