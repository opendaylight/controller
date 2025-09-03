/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import org.apache.pekko.dispatch.ControlMessage;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This interface is used to pass the unit of work via the actors mailbox. The command can alter behavior of the actor
 * by returning a new behavior. This work will be prioritized before other messages.
 *
 * @param <T> the type of associated {@link BackendInfo}
 */
@FunctionalInterface
public interface InternalCommand<T extends BackendInfo> extends ControlMessage {
    /**
     * Run command actions.
     *
     * @param currentBehavior Current Behavior
     * @return Next behavior to use in the client actor
     */
    @Nullable ClientActorBehavior<T> execute(@NonNull ClientActorBehavior<T> currentBehavior);
}
