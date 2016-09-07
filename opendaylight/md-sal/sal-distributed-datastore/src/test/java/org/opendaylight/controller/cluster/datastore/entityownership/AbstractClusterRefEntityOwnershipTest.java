/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.entityownership;

import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import com.typesafe.config.ConfigFactory;
import java.io.IOException;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class AbstractClusterRefEntityOwnershipTest extends AbstractEntityOwnershipTest {

    private static ActorSystem system;

    @BeforeClass
    public static void setUpClass() throws IOException {
        system = ActorSystem.create("test", ConfigFactory.load().getConfig("test"));
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
