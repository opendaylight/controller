/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import java.io.Serializable;
import org.eclipse.jdt.annotation.NonNull;

/**
 * Request a shard to report the clients it knows about. Shard is required to respond with {@link GetKnownClientsReply}.
 */
public final class GetKnownClients implements Serializable {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public static final @NonNull GetKnownClients INSTANCE = new GetKnownClients();

    private GetKnownClients() {
        // Hidden on purpose
    }

    @java.io.Serial
    private Object readResolve() {
        return INSTANCE;
    }
}
