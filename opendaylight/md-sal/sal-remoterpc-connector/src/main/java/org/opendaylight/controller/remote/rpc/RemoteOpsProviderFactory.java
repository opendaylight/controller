/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;

import akka.actor.ActorSystem;
import org.opendaylight.mdsal.dom.api.DOMActionProviderService;
import org.opendaylight.mdsal.dom.api.DOMActionService;
import org.opendaylight.mdsal.dom.api.DOMRpcProviderService;
import org.opendaylight.mdsal.dom.api.DOMRpcService;

public final class RemoteOpsProviderFactory {
    private RemoteOpsProviderFactory() {

    }

    public static RemoteOpsProvider createInstance(final DOMRpcProviderService rpcProviderService,
                                                   final DOMRpcService rpcService, final ActorSystem actorSystem,
                                                   final RemoteOpsProviderConfig config,
                                                   final DOMActionProviderService actionProviderService,
                                                   final DOMActionService actionService) {

        return new RemoteOpsProvider(actorSystem, rpcProviderService, rpcService, config,
                actionProviderService, actionService);
    }
}
