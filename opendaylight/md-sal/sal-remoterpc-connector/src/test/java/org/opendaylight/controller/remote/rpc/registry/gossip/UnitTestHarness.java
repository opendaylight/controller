/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry.gossip;

import com.typesafe.config.ConfigFactory;
import java.time.Duration;
import java.util.ServiceLoader;
import org.apache.pekko.actor.Address;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.opendaylight.controller.pekko.support.ActorSystemInstance;
import org.opendaylight.controller.pekko.support.dagger.ActorSystemCreator;

public abstract class UnitTestHarness {

    static ActorSystemInstance.WithShutdown SYSTEM_INSTANCE;

    @BeforeAll
    public static final void beforeAll() throws Exception {
        final var creator = ServiceLoader.load(ActorSystemCreator.class).findFirst().orElseThrow();
        SYSTEM_INSTANCE = creator.createInstance("opendaylight-rpc", ConfigFactory.load().getConfig("unit-test"),
            new ActorSystemCreator.Callbacks() {
                @Override
                public void onLocalDown() {
                    // no-op
                }

                @Override
                public void onRemoteQuarantined(final Address quarantinedBy) {
                    // no-op
                }
            });
    }

    @AfterAll
    public static final void afterAll() throws Exception {
        if (SYSTEM_INSTANCE != null) {
            SYSTEM_INSTANCE.shutdownAndWait(Duration.ofSeconds(10));
        }
    }
}
