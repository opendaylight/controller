/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import java.io.Serializable;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;

@Beta
public final class DisableTransactionTracking implements Serializable {
    private static final long serialVersionUID = 1L;

    private final ClientIdentifier clientId;

    public DisableTransactionTracking(final ClientIdentifier clientId) {
        this.clientId = requireNonNull(clientId, "clientID cannot be null");
    }

    public ClientIdentifier getClientId() {
        return clientId;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("clientId", clientId).toString();
    }
}
