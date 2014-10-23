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
import org.opendaylight.controller.sal.binding.impl.RootBindingAwareBroker;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.RpcProvisionRegistry;
import org.opendaylight.controller.sal.core.api.notify.NotificationPublishService;
import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;

public class BindingDomConnectorDeployer {

    private static BindingIndependentMappingService mappingService;

    public static BindingIndependentConnector tryToDeployConnector(final RootBindingAwareBroker baBroker,
            final ProviderSession domSession) {
        checkNotNull(baBroker);
        checkNotNull(domSession);
        final BindingIndependentConnector connector = createConnector(mappingService);
        return connector;
    }

    public static BindingIndependentConnector createConnector(final BindingIndependentMappingService mappingService) {
        final BindingIndependentConnector connector = new BindingIndependentConnector();
        connector.setMappingService(mappingService);
        return connector;
    }

    public static BindingIndependentConnector createConnector(final BindingIndependentConnector source) {
        final BindingIndependentConnector connector = new BindingIndependentConnector();
        connector.setMappingService(source.getMappingService());
        return connector;
    }

    /**
     * FIXME: Remove after Mount point code path is fully migrated to non-legacy APIs.
     * @deprecated This method provides forwarding for deprecated APIs, thus should be used only
     * from legacy support code.
     */
    @Deprecated
    public static void startDataForwarding(final BindingIndependentConnector connector, final DataProviderService baService,
            final ProviderSession domContext) {
        startDataForwarding(connector, baService,
                domContext.getService(org.opendaylight.controller.sal.core.api.data.DataProviderService.class));
    }

    public static void startRpcForwarding(final BindingIndependentConnector connector,
            final RpcProviderRegistry rpcProviderRegistry, final ProviderSession domProviderContext) {
        startRpcForwarding(connector, rpcProviderRegistry, domProviderContext.getService(RpcProvisionRegistry.class));

    }

    public static void startNotificationForwarding(final BindingIndependentConnector connector, final NotificationProviderService provider,final ProviderSession domProviderContext) {
        startNotificationForwarding(connector, provider, domProviderContext.getService(NotificationPublishService.class));
    }

    public static void startRpcForwarding(final BindingIndependentConnector connector, final RpcProviderRegistry baService,
            final RpcProvisionRegistry domService) {
        if (connector.isRpcForwarding()) {
            return;
        }

        connector.setDomRpcRegistry(domService);
        connector.setBindingRpcRegistry(baService);
        connector.startRpcForwarding();
    }

    /**
     * FIXME: Remove after Mount point code path is fully migrated to non-legacy APIs.
     * @deprecated This method provides forwarding for deprecated APIs, thus should be used only
     * from legacy support code. Was replaced by {@link org.opendaylight.controller.md.sal.binding.impl.ForwardedBackwardsCompatibleDataBroker}.
     **/
    @Deprecated
    public static void startDataForwarding(final BindingIndependentConnector connector, final DataProviderService baService,
            final org.opendaylight.controller.sal.core.api.data.DataProviderService domService) {
        if (connector.isDataForwarding()) {
            return;
        }

        connector.setBindingDataService(baService);
        connector.setDomDataService(domService);
        connector.startDataForwarding();
    }

    public static void startNotificationForwarding(final BindingIndependentConnector connector,
            final NotificationProviderService baService, final NotificationPublishService domService) {
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
