/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import akka.actor.ActorPath;
import akka.actor.ActorRef;
import com.google.common.base.Preconditions;

import java.io.Serializable;

/**
 * Successful reply to a {@link RegisterDataTreeChangeListener} request.
 */
public final class RegisterDataTreeChangeListenerReply implements Serializable {
    private static final long serialVersionUID = 1L;
    private final ActorRef listenerRegistrationPath;

    public RegisterDataTreeChangeListenerReply(final ActorRef listenerRegistrationPath) {
        this.listenerRegistrationPath = Preconditions.checkNotNull(listenerRegistrationPath);
    }

    public ActorPath getListenerRegistrationPath() {
        return listenerRegistrationPath.path();
    }
}
