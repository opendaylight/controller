/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.opendaylight.controller.cluster.access.client.ConnectionEntry;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.yangtools.concepts.Identifiable;

abstract class ProxyReconnectCohort implements Identifiable<LocalHistoryIdentifier> {

    abstract void replaySuccessfulRequests(Iterable<ConnectionEntry> previousEntries);

    abstract ProxyHistory finishReconnect();

    abstract void replayRequest(Request<?, ?> request, Consumer<Response<?, ?>> callback,
            BiConsumer<Request<?, ?>, Consumer<Response<?, ?>>> replayTo) throws RequestException;
}
