/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.remote_rpc_connector;

import akka.actor.ActorSystem;
import org.opendaylight.controller.remote.rpc.RemoteRpcProviderConfig;
import org.opendaylight.controller.remote.rpc.RemoteRpcProviderFactory;
import org.opendaylight.controller.sal.core.api.Broker;
import org.osgi.framework.BundleContext;

public class RemoteRPCBrokerModule extends org.opendaylight.controller.config.yang.config.remote_rpc_connector.AbstractRemoteRPCBrokerModule {
    private BundleContext bundleContext;
    public RemoteRPCBrokerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public RemoteRPCBrokerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.remote_rpc_connector.RemoteRPCBrokerModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public boolean canReuseInstance(AbstractRemoteRPCBrokerModule oldModule) {
        return true;
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        Broker broker = getDomBrokerDependency();

        ActorSystem actorSystem = getActorSystemProviderDependency().getActorSystem();
        RemoteRpcProviderConfig config = new RemoteRpcProviderConfig.Builder(actorSystem.name())
                .metricCaptureEnabled(getEnableMetricCapture())
                .mailboxCapacity(getBoundedMailboxCapacity())
                .build();

        return RemoteRpcProviderFactory.createInstance(broker, bundleContext, actorSystem, config);
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
