/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.persistence;

import akka.persistence.japi.snapshot.JavaSnapshotStoreSpec;
import com.typesafe.config.ConfigFactory;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
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
public class LocalSnapshotStoreSpecTest extends JavaSnapshotStoreSpec {
    private static final long serialVersionUID = 1L;
    static final File SNAPSHOT_DIR = new File("target/snapshots");

    public LocalSnapshotStoreSpecTest() {
        super(ConfigFactory.load("LocalSnapshotStoreTest.conf"));
    }

    @Override
    public void afterAll() {
        super.afterAll();
        FileUtils.deleteQuietly(SNAPSHOT_DIR);
    }

    static void cleanSnapshotDir() {
        if (!SNAPSHOT_DIR.exists()) {
            return;
        }

        try {
            FileUtils.cleanDirectory(SNAPSHOT_DIR);
        } catch (IOException e) {
            // Ignore
        }
    }

    static void createSnapshotDir() {
        if (!SNAPSHOT_DIR.exists() && !SNAPSHOT_DIR.mkdirs()) {
            throw new RuntimeException("Failed to create " + SNAPSHOT_DIR);
        }

        cleanSnapshotDir();
    }
}
