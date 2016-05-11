/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors.client;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;

public final class ClientIdResponse<T extends FrontendType> {
    private final ClientIdentifier<T> clientId;

    ClientIdResponse(final ClientIdentifier<T> clientId) {
        this.clientId = Preconditions.checkNotNull(clientId);
    }

    public ClientIdentifier<T> getClientid() {
        return clientId;
    }
}