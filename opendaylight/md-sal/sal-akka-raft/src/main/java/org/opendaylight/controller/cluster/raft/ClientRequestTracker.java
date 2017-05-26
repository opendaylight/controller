/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft;

import akka.actor.ActorRef;
import org.opendaylight.yangtools.concepts.Identifier;

public interface ClientRequestTracker {
    /**
     * Returns the client actor that should be sent a response when consensus is achieved.
     *
     * @return the client actor
     */
    ActorRef getClientActor();

    /**
     * Returns the identifier of the object that is to be replicated. For example a transaction identifier in the case
     * of a transaction.
     *
     * @return the identifier
     */
    Identifier getIdentifier();

    /**
     * Returns the index of the log entry that is to be replicated.
     *
     * @return the index
     */
    long getIndex();

}
