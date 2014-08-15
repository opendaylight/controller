/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.md.sal.binding.impl;

import java.util.concurrent.ExecutorService;

import org.opendaylight.controller.sal.binding.codegen.impl.SingletonHolder;
import org.opendaylight.controller.sal.binding.impl.RootDataBrokerImpl;
import org.opendaylight.controller.sal.binding.impl.connect.dom.BindingDomConnectorDeployer;
import org.opendaylight.controller.sal.binding.impl.connect.dom.BindingIndependentConnector;
import org.opendaylight.controller.sal.binding.impl.forward.DomForwardedDataBrokerImpl;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;

/**
*
*/
public final class DataBrokerImplModule extends
        org.opendaylight.controller.config.yang.md.sal.binding.impl.AbstractDataBrokerImplModule {

    public DataBrokerImplModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public DataBrokerImplModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            final DataBrokerImplModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void validate() {
        super.validate();
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        RootDataBrokerImpl dataBindingBroker;


        ExecutorService listeningExecutor = SingletonHolder.getDefaultCommitExecutor();
        BindingIndependentMappingService potentialMapping = getMappingServiceDependency();
        if (getDomBrokerDependency() != null && potentialMapping != null) {

            dataBindingBroker = createDomConnectedBroker(listeningExecutor,potentialMapping);
        } else {
            dataBindingBroker = createStandAloneBroker(listeningExecutor);
        }
        dataBindingBroker.registerRuntimeBean(getRootRuntimeBeanRegistratorWrapper());
        dataBindingBroker.setNotificationExecutor(SingletonHolder.getDefaultChangeEventExecutor());
        return dataBindingBroker;
    }


    private RootDataBrokerImpl createStandAloneBroker(final ExecutorService listeningExecutor) {
        RootDataBrokerImpl broker = new RootDataBrokerImpl();
        broker.setExecutor(listeningExecutor);
        return broker;
    }

    private RootDataBrokerImpl createDomConnectedBroker(final ExecutorService listeningExecutor, final BindingIndependentMappingService mappingService) {
        DomForwardedDataBrokerImpl forwardedBroker = new DomForwardedDataBrokerImpl();
        forwardedBroker.setExecutor(listeningExecutor);
        BindingIndependentConnector connector = BindingDomConnectorDeployer.createConnector(mappingService);
        getDomBrokerDependency().registerProvider(forwardedBroker, null);
        ProviderSession domContext = forwardedBroker.getDomProviderContext();
        forwardedBroker.setConnector(connector);
        forwardedBroker.setDomProviderContext(domContext);
        forwardedBroker.startForwarding();
        return forwardedBroker;
    }

}
