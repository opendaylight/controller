/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft;

import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.opendaylight.yangtools.util.AbstractStringIdentifier;

public abstract class AbstractActorTest {
    protected static final class MockIdentifier extends AbstractStringIdentifier<MockIdentifier> {
        private static final long serialVersionUID = 1L;

        public MockIdentifier(final String string) {
            super(string);
        }
    }

    private static ActorSystem system;

    @BeforeClass
    public static void setUpClass() throws Exception {
        deleteJournal();
        System.setProperty("shard.persistent", "false");
        system = ActorSystem.create("test", getConfig());
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        deleteJournal();
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    protected ActorSystem getSystem() {
        return system;
    }

    protected static void deleteJournal() throws IOException {
        File journal = new File("journal");

        if (journal.exists()) {
            FileUtils.deleteDirectory(journal);
        }
    }

    private static Config getConfig() {
        Map<String, String> overrides = new HashMap<>();
        overrides.put("akka.actor.provider", "cluster");
        return ConfigFactory.parseMap(overrides).withFallback(ConfigFactory.load());
    }

}
