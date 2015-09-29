/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc;

import akka.actor.ActorSystem;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.sal.core.api.Broker;
import org.osgi.framework.BundleContext;

public class RemoteRpcProviderFactory {
    public static RemoteRpcProvider createInstance(final Broker broker, final BundleContext bundleContext,
            final ActorSystem actorSystem, final RemoteRpcProviderConfig config) {

        final RemoteRpcProvider rpcProvider = new RemoteRpcProvider(actorSystem, (DOMRpcProviderService) broker, config);

        broker.registerProvider(rpcProvider);
        return rpcProvider;
    }
}
