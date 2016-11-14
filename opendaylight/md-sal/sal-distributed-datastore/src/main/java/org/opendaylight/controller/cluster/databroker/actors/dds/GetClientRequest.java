/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import akka.actor.ActorRef;
import com.google.common.base.Preconditions;

/**
 * Request the ClientIdentifier from a particular actor. Response is an instance of {@link DataStoreClient}.
 *
 * @author Robert Varga
 */
final class GetClientRequest {
    private final ActorRef replyTo;

    GetClientRequest(final ActorRef replyTo) {
        this.replyTo = Preconditions.checkNotNull(replyTo);
    }

    ActorRef getReplyTo() {
        return replyTo;
    }
}
