/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import static java.util.Objects.requireNonNull;

import akka.actor.ActorPath;
import akka.actor.ActorRef;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;

/**
 * Successful reply to a notification listener registration request providing initial state.
 */
public class RegisterDataTreeNotificationListenerReplyWithInitialState {

    private final ActorRef listenerRegistrationPath;
    private final String shardName;
    private final DataTreeCandidate initialData;

    public RegisterDataTreeNotificationListenerReplyWithInitialState(final ActorRef listenerRegistrationPath,
            final String shardName, final DataTreeCandidate initialData) {
        this.listenerRegistrationPath = requireNonNull(listenerRegistrationPath);
        this.shardName = requireNonNull(shardName);
        this.initialData = initialData;
    }

    public ActorPath getListenerRegistrationPath() {
        return listenerRegistrationPath.path();
    }

    public DataTreeCandidate getInitialData() {
        return initialData;
    }

    public String getShardName() {
        return shardName;
    }
}
