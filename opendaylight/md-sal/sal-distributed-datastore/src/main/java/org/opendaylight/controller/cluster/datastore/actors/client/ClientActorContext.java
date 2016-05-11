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
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.yangtools.concepts.Identifiable;

/**
 * An actor context associated with this {@link AbstractClientActor}
 * @author user
 *
 * @param <T> Front type
 *
 * @author Robert Varga
 */
@Beta
public final class ClientActorContext<T extends FrontendType> extends AbstractClientActorContext implements Identifiable<ClientIdentifier<T>> {
    private final ClientIdentifier<T> identifier;

    ClientActorContext(final ActorRef self, final String persistenceId, final ClientIdentifier<T> identifier) {
        super(self, persistenceId);
        this.identifier = Preconditions.checkNotNull(identifier);
    }

    @Override
    public ClientIdentifier<T> getIdentifier() {
        return identifier;
    }
}
