/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.registry.listener.owner.command;

import static java.util.Objects.requireNonNull;

import akka.cluster.ddata.LWWRegister;
import akka.cluster.ddata.typed.javadsl.Replicator.SubscribeResponse;
import org.eclipse.jdt.annotation.NonNull;

/**
 * Notification from distributed-data sent to the SingleEntityListenerActor when owner changes for the tracked entity.
 */
public final class OwnerChanged extends ListenerCommand {
    private final @NonNull SubscribeResponse<LWWRegister<String>> response;

    public OwnerChanged(final SubscribeResponse<LWWRegister<String>> response) {
        this.response = requireNonNull(response);
    }

    public @NonNull SubscribeResponse<LWWRegister<String>> getResponse() {
        return response;
    }
}
