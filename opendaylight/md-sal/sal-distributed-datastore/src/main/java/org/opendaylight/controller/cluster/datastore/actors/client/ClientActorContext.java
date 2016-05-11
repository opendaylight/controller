/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors.client;

import akka.actor.ActorRef;
import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.yangtools.concepts.Identifiable;

/**
 * An actor context associated with this {@link AbstractClientActor}
 *
 * @author Robert Varga
 */
@Beta
public final class ClientActorContext extends AbstractClientActorContext implements Identifiable<ClientIdentifier> {
    private final ClientIdentifier identifier;

    ClientActorContext(final ActorRef self, final String persistenceId, final ClientIdentifier identifier) {
        super(self, persistenceId);
        this.identifier = Preconditions.checkNotNull(identifier);
    }

    @Override
    public ClientIdentifier getIdentifier() {
        return identifier;
    }
}
