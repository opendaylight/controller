/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.entityownership;

import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import com.typesafe.config.ConfigFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class AbstractClusterRefEntityOwnershipTest extends AbstractEntityOwnershipTest {

    private static ActorSystem system;

    @BeforeClass
    public static void setUpClass() {
        system = ActorSystem.create("test", ConfigFactory.load().getConfig("test-config"));
    }

    @AfterClass
    public static void tearDownClass() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    protected static ActorSystem getSystem() {
        return system;
    }
}
