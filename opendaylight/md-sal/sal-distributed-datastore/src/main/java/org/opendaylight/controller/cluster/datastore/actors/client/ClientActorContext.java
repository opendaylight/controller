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
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.concepts.Identifier;

/**
 * An actor context associated with this {@link AbstractClientActor}
 *
 * @author Robert Varga
 */
@Beta
public final class ClientActorContext extends AbstractClientActorContext implements Identifiable<ClientIdentifier> {
    private final Map<Identifier, TargetRequestQueue> requests = new HashMap<>();
    private final ClientIdentifier identifier;

    ClientActorContext(final ActorRef self, final String persistenceId, final ClientIdentifier identifier) {
        super(self, persistenceId);
        this.identifier = Preconditions.checkNotNull(identifier);
    }

    @Override
    public ClientIdentifier getIdentifier() {
        return identifier;
    }

    void enqueueRequest(final Request<?, ?> request, final Object context) {
        final Identifier target = request.getTarget();

        TargetRequestQueue queue = requests.get(target);
        if (queue == null) {
            Preconditions.checkArgument(request.getSequence() == 0, "Wrong sequence in request %s, expected 0", request);
            queue = new TargetRequestQueue();
            requests.put(target, queue);
        }

        queue.append(request);
    }
}
