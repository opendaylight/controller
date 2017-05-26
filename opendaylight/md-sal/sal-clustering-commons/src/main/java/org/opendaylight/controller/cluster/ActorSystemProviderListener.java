/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster;

import akka.actor.ActorSystem;
import java.util.EventListener;

/**
 * Listener interface for notification of ActorSystem changes from an ActorSystemProvider.
 *
 * @author Thomas Pantelis
 */
public interface ActorSystemProviderListener extends EventListener {
    /**
     * Method called when the current actor system is about to be shutdown.
     */
    void onPreShutdownActorSystem();

    /**
     * Method called when the current actor system is shutdown and a new actor system is created. This method
     * is always preceded by a call to {@link #onPreShutdownActorSystem}.
     *
     * @param actorSytem the new ActorSystem
     */
    void onNewActorSystem(ActorSystem actorSytem);
}
