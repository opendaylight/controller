/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.akka.eos.registry.listener.owner.command;

import akka.cluster.ddata.LWWRegister;
import akka.cluster.ddata.typed.javadsl.Replicator;

/**
 * Notification from distributed-data sent to the SingleEntityListenerActor when owner changes for the tracked entity.
 */
public class OwnerChanged implements ListenerCommand {

    private final Replicator.SubscribeResponse<LWWRegister<String>> response;

    public OwnerChanged(final Replicator.SubscribeResponse<LWWRegister<String>> response) {
        this.response = response;
    }

    public Replicator.SubscribeResponse<LWWRegister<String>> getResponse() {
        return response;
    }
}
