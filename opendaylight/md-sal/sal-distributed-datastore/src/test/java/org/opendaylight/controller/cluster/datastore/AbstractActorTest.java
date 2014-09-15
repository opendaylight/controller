/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.File;
import java.io.IOException;

public abstract class AbstractActorTest {
    private static ActorSystem system;

    @BeforeClass
    public static void setUpClass() throws IOException {

        System.setProperty("shard.persistent", "false");
        system = ActorSystem.create("test");

        deletePersistenceFiles();
    }

    @AfterClass
    public static void tearDownClass() throws IOException {
        JavaTestKit.shutdownActorSystem(system);
        system = null;

        deletePersistenceFiles();
    }

    protected static void deletePersistenceFiles() throws IOException {
        File journal = new File("journal");

        if(journal.exists()) {
            FileUtils.deleteDirectory(journal);
        }

        File snapshots = new File("snapshots");

        if(snapshots.exists()){
            FileUtils.deleteDirectory(snapshots);
        }

    }

    protected ActorSystem getSystem() {
        return system;
    }

}
