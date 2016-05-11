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

public final class ClientActorContext<T extends FrontendType> extends AbstractClientActorContext  {
    private final ClientIdentifier<T> identifier;

    ClientActorContext(final String persistenceId, final ClientIdentifier<T> identifier) {
        super(persistenceId);
        this.identifier = Preconditions.checkNotNull(identifier);
    }

    ClientIdentifier<T> getIdentifier() {
        return identifier;
    }
}
