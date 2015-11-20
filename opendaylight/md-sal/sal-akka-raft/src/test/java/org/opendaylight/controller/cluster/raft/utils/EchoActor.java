/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.utils;

import akka.actor.UntypedActor;

/**
 * The EchoActor simply responds back with the same message that it receives
 */
public class EchoActor extends UntypedActor {

    @Override
    public void onReceive(Object message) throws Exception {
        getSender().tell(message, getSelf());
    }
}
