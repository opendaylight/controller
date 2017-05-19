/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import java.util.Collection;
import org.opendaylight.controller.cluster.access.client.ConnectionEntry;

/**
 * Interface exposed by {@link AbstractClientHistory} to {@link DistributedDataStoreClientBehavior} for the sole
 * purpose of performing a connection switchover.
 *
 * @author Robert Varga
 */
abstract class HistoryReconnectCohort implements AutoCloseable {
    abstract ProxyReconnectCohort getProxy();

    abstract void replayRequests(Collection<ConnectionEntry> previousEntries);

    @Override
    public abstract void close();
}
