/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster;

import akka.actor.ActorSystem;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.concepts.ListenerRegistration;

/**
 * Interface that provides an akka ActorSystem instance.
 *
 * @author Thomas Pantelis
 */
public interface ActorSystemProvider {

    /**
     * Returns the ActorSystem.
     *
     * @return the ActorSystem.
     */
    @NonNull ActorSystem getActorSystem();

    /**
     * Register a listener for ActorSystem lifecycle events.
     *
     * @param listener the ActorSystemProviderListener to register
     * @return a ListenerRegistration instance to be used to unregister
     */
    ListenerRegistration<ActorSystemProviderListener> registerActorSystemProviderListener(
            @NonNull ActorSystemProviderListener listener);
}
