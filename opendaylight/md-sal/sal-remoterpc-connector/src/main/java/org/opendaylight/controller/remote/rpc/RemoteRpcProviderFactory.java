/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc;


import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.RpcProvisionRegistry;
import org.osgi.framework.BundleContext;

public class RemoteRpcProviderFactory {
    public static RemoteRpcProvider createInstance(final Broker broker, final BundleContext bundleContext){

      ActorSystemFactory.createInstance(bundleContext);
      RemoteRpcProvider rpcProvider =
          new RemoteRpcProvider(ActorSystemFactory.getInstance(), (RpcProvisionRegistry) broker);
      broker.registerProvider(rpcProvider);
      return rpcProvider;
    }
}
