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
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;

/**
 * Customizable extension layer between the top level TopologyManager and NodeManager
 */
@Beta
public interface TopologyManagerCallback extends NodeListener, Receiver, RoleChangeListener {

    interface TopologyManagerCallbackFactory {
        TopologyManagerCallback create(ActorSystem actorSystem, DataBroker dataBroker, String topologyId, List<String> remotePaths);
    }
}

