/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import org.apache.pekko.actor.ActorPath;
import org.apache.pekko.actor.ActorRef;

/**
 * Successful reply to a notification listener registration request.
 *
 * @author Thomas Pantelis
 */
public final class RegisterDataTreeNotificationListenerReply implements Serializable {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final ActorRef listenerRegistrationPath;

    public RegisterDataTreeNotificationListenerReply(final ActorRef listenerRegistrationPath) {
        this.listenerRegistrationPath = requireNonNull(listenerRegistrationPath);
    }

    public ActorPath getListenerRegistrationPath() {
        return listenerRegistrationPath.path();
    }
}
