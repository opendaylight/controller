/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.topology;

import akka.actor.ActorSystem;
import akka.actor.TypedActor.Receiver;
import com.google.common.annotations.Beta;

/**
 * Customizable layer that handles communication with your application.
 */
@Beta
public interface NodeManagerCallback extends InitialStateProvider, NodeListener, Receiver {

    interface NodeManagerCallbackFactory<M> {
        NodeManagerCallback create(String nodeId, String topologyId, ActorSystem actorSystem);
    }
}

