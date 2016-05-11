/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors.client;

import akka.actor.ActorRef;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;

public abstract class ClientActorBehavior<T extends FrontendType> extends RecoveredClientActorBehavior<ClientActorContext<T>, T> {
    protected ClientActorBehavior(final ClientActorContext<T> context) {
        super(context);
    }

    protected final ClientIdentifier<T> getIdentifier() {
        return context().getIdentifier();
    }

    @Override
    final ClientActorBehavior<T> onReceiveCommand(final Object command) {
        if (command instanceof ClientIdRequest) {
            ((ClientIdRequest) command).getReplyTo().tell(new ClientIdResponse<>(getIdentifier()), ActorRef.noSender());
            return this;
        } else {
            return handleCommand(command);
        }
    }

    protected abstract ClientActorBehavior<T> handleCommand(Object command);
}