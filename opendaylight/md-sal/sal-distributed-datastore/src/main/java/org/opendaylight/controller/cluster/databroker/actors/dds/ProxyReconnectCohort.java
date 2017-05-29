/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import java.util.Collection;
import java.util.function.Consumer;
import org.opendaylight.controller.cluster.access.client.ConnectionEntry;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.yangtools.concepts.Identifiable;

abstract class ProxyReconnectCohort implements Identifiable<LocalHistoryIdentifier> {

    abstract void replayRequests(Collection<ConnectionEntry> previousEntries);

    abstract ProxyHistory finishReconnect();

    abstract void replayEntry(ConnectionEntry entry, Consumer<ConnectionEntry> replayTo) throws RequestException;

    abstract void forwardEntry(ConnectionEntry entry, Consumer<ConnectionEntry> forwardTo) throws RequestException;
}
