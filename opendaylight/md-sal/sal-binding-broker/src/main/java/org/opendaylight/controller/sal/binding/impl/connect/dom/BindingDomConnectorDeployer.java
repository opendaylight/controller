/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.impl.connect.dom;

import static com.google.common.base.Preconditions.checkNotNull;

import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;
import org.opendaylight.controller.sal.binding.impl.RootBindingAwareBroker;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.RpcProvisionRegistry;
import org.opendaylight.controller.sal.core.api.notify.NotificationPublishService;

public class BindingDomConnectorDeployer {

    private static BindingIndependentMappingService mappingService;

    public static BindingIndependentConnector tryToDeployConnector(RootBindingAwareBroker baBroker,
            ProviderSession domSession) {
        checkNotNull(baBroker);
        checkNotNull(domSession);
        BindingIndependentConnector connector = createConnector(mappingService);
        return connector;
    }

    public static BindingIndependentConnector createConnector(BindingIndependentMappingService mappingService) {
        BindingIndependentConnector connector = new BindingIndependentConnector();
        connector.setMappingService(mappingService);
        return connector;
    }

    public static BindingIndependentConnector createConnector(BindingIndependentConnector source) {
        BindingIndependentConnector connector = new BindingIndependentConnector();
        connector.setMappingService(source.getMappingService());
        return connector;
    }

    public static void startDataForwarding(BindingIndependentConnector connector, DataProviderService baService,
            ProviderSession domContext) {
        startDataForwarding(connector, baService,
                domContext.getService(org.opendaylight.controller.sal.core.api.data.DataProviderService.class));
    }

    public static void startRpcForwarding(BindingIndependentConnector connector,
            RpcProviderRegistry rpcProviderRegistry, ProviderSession domProviderContext) {
        startRpcForwarding(connector, rpcProviderRegistry, domProviderContext.getService(RpcProvisionRegistry.class));

    }

    public static void startNotificationForwarding(BindingIndependentConnector connector, NotificationProviderService provider,ProviderSession domProviderContext) {
        startNotificationForwarding(connector, provider, domProviderContext.getService(NotificationPublishService.class));
    }

    public static void startRpcForwarding(BindingIndependentConnector connector, RpcProviderRegistry baService,
            RpcProvisionRegistry domService) {
        if (connector.isRpcForwarding()) {
            return;
        }

        connector.setDomRpcRegistry(domService);
        connector.setBindingRpcRegistry(baService);
        connector.startRpcForwarding();
    }

    public static void startDataForwarding(BindingIndependentConnector connector, DataProviderService baService,
            org.opendaylight.controller.sal.core.api.data.DataProviderService domService) {
        if (connector.isDataForwarding()) {
            return;
        }

        connector.setBindingDataService(baService);
        connector.setDomDataService(domService);
        connector.startDataForwarding();
    }

    public static void startNotificationForwarding(BindingIndependentConnector connector, 
            NotificationProviderService baService, NotificationPublishService domService) {
        if(connector.isNotificationForwarding()) {
            return;
        }
        connector.setBindingNotificationService(baService);
        connector.setDomNotificationService(domService);
        connector.startNotificationForwarding();
    }

    //
    // public static BindingIndependentMappingService getGlobalMappingService()
    // {
    // return mappingService;
    // }
    //
    // protected static BindingIndependentMappingService
    // setGlobalMappingService(BindingIndependentMappingService service) {
    // mappingService= service;
    // return mappingService;
    // }
    //
    // public static BindingIndependentConnector
    // tryToDeployConnector(MountProviderInstance baMount,MountProvisionInstance
    // domMount) {
    //
    //
    // return null;
    // }

}
