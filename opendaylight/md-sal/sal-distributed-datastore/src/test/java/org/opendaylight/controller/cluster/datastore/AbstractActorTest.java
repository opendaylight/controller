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
import java.io.IOException;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public abstract class AbstractActorTest extends AbstractTest {
    private static ActorSystem system;

    @BeforeClass
    public static void setUpClass() throws IOException {
        System.setProperty("shard.persistent", "false");
        system = ActorSystem.create("test");
    }

    @AfterClass
    public static void tearDownClass() throws IOException {
        JavaTestKit.shutdownActorSystem(system);
        system = null;
    }

    protected static ActorSystem getSystem() {
        return system;
    }
}
