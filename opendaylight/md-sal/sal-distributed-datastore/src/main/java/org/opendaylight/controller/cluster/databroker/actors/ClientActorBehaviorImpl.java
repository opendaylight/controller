/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors;

import akka.actor.ActorRef;
import org.opendaylight.controller.cluster.datastore.actors.client.ClientActorBehavior;
import org.opendaylight.controller.cluster.datastore.actors.client.ClientActorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ClientActorBehaviorImpl extends ClientActorBehavior<DistributedDataStoreFrontend> {
    private static final Logger LOG = LoggerFactory.getLogger(ClientActorBehaviorImpl.class);

    private ClientActorBehaviorImpl(final ClientActorContext<DistributedDataStoreFrontend> context) {
        super(context);
    }

    static ClientActorBehaviorImpl create(final ClientActorContext<DistributedDataStoreFrontend> context) {
        return new ClientActorBehaviorImpl(context);
    }

    @Override
    protected ClientActorBehavior<DistributedDataStoreFrontend> onReceiveCommand(final Object command) {
        if (command instanceof ClientIdRequest) {
            ((ClientIdRequest) command).getReplyTo().tell(new ClientIdResponse<>(getIdentifier()), ActorRef.noSender());
        } else {
            LOG.warn("{}: ignoring unhandled command {}", persistenceId(), command);
        }

        return this;
    }
}
