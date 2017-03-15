/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.ResponseEnvelope;

/**
 * Util class to access package private members in cds-access-client for test purposes.
 */
public class AccessClientUtil {

    public static ClientActorContext createClientActorContext(final ActorSystem system, final ActorRef actor,
                                                              final ClientIdentifier id, final String persistenceId) {
        return new ClientActorContext(actor, system.scheduler(), system.dispatcher(), persistenceId, id);
    }

    public static void completeRequest(final AbstractClientConnection<? extends BackendInfo> connection,
                                       final ResponseEnvelope<?> envelope) {
        connection.receiveResponse(envelope);
    }

}