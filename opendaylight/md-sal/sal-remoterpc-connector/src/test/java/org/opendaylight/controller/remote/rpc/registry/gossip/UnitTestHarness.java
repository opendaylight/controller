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
import org.apache.pekko.actor.Address;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.opendaylight.controller.pekko.support.spi.DefaultActorSystemInstance;

public abstract class UnitTestHarness {

    static DefaultActorSystemInstance SYSTEM_INSTANCE;

    @BeforeAll
    public static final void beforeAll() throws Exception {
        SYSTEM_INSTANCE = new DefaultActorSystemInstance(
            "opendaylight-rpc",
            ConfigFactory.load().getConfig("unit-test"),
            new DefaultActorSystemInstance.Callbacks() {
                @Override
                public void onLocalDown() {
                    // no-op
                }

                @Override
                public void onRemoteQuarantined(final Address quarantinedBy) {
                    // no-op
                }
            }, UnitTestHarness.class.getClassLoader());
    }

    @AfterAll
    public static final void afterAll() throws Exception {
        if (SYSTEM_INSTANCE != null) {
            SYSTEM_INSTANCE.shutdownAndWait(Duration.ofSeconds(10));
        }
    }
}
