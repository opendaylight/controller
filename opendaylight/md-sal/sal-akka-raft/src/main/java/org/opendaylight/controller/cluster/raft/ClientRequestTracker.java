/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft;

import akka.actor.ActorRef;

public interface ClientRequestTracker {
    /**
     * The client actor who is waiting for a response
     *
     * @return
     */
    ActorRef getClientActor();

    /**
     *
     * @return
     */
    String getIdentifier();

    /**
     * The index of the log entry which needs to be replicated
     *
     * @return
     */
    long getIndex();

}
