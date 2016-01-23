/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import akka.actor.ActorPath;
import akka.actor.ActorRef;

public class RegisterChangeListenerReply{
    private final ActorRef listenerRegistration;

    public RegisterChangeListenerReply(final ActorRef listenerRegistration) {
        this.listenerRegistration = listenerRegistration;
    }

    public ActorPath getListenerRegistrationPath() {
        return listenerRegistration.path();
    }
}
