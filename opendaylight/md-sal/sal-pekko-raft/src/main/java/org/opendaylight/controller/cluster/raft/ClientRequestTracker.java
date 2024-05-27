/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import org.apache.pekko.actor.ActorRef;
import org.opendaylight.yangtools.concepts.Identifier;

/**
 * Consensus forwarding tracker.
 *
 * @param clientActor the client actor that should be sent a response when consensus is achieved
 * @param identifier the identifier of the object that is to be replicated. For example a transaction identifier in the
 *        case of a transaction
 * @param logIndex the index of the log entry that is to be replicated
 */
public record ClientRequestTracker(long logIndex, ActorRef clientActor, Identifier identifier) {

}
