/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.akka.impl;

import com.typesafe.config.Config;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.pekko.support.spi.AbstractActorSystemInstance;

public class ClusterActorSystemInstance extends AbstractActorSystemInstance implements AutoCloseable {
    @NonNullByDefault
    public ClusterActorSystemInstance(final Config config, final Callbacks callbacks, final ClassLoader classLoader) {
        super("opendaylight-cluster-data", config, callbacks, classLoader);
    }

    @Override
    public final void close() throws TimeoutException, InterruptedException {
        shutdownAndWait(Duration.ofSeconds(10));
    }
}
