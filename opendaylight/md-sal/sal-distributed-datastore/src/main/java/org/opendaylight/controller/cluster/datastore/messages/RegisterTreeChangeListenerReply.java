/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import com.google.common.base.Preconditions;
import akka.actor.ActorRef;
import java.io.Serializable;

public final class RegisterTreeChangeListenerReply implements Serializable {
    private static final long serialVersionUID = 1L;
    private final ActorRef listenerRegistrationPath;

    public RegisterTreeChangeListenerReply(final ActorRef listenerRegistrationPath) {
        this.listenerRegistrationPath = Preconditions.checkNotNull(listenerRegistrationPath);
    }

    public ActorRef getListenerRegistrationPath() {
        return listenerRegistrationPath;
    }
}
