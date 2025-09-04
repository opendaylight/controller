/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableSet;
import java.io.Serializable;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;

/**
 * Reply to {@link GetKnownClients}.
 */
public final class GetKnownClientsReply implements Serializable {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final @NonNull ImmutableSet<ClientIdentifier> clients;

    public GetKnownClientsReply(final ImmutableSet<ClientIdentifier> clients) {
        this.clients = requireNonNull(clients);
    }

    public @NonNull ImmutableSet<ClientIdentifier> getClients() {
        return clients;
    }
}
