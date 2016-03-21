/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.blueprint.ext;

import com.google.common.base.Preconditions;
import java.util.Arrays;
import java.util.List;
import org.apache.aries.blueprint.ext.ComponentFactoryMetadata;
import org.apache.aries.blueprint.services.ExtendedBlueprintContainer;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory metadata corresponding to the "routed-rpc-implementation" element that registers an RPC
 * implementation with the RpcProviderRegistry and provides the RoutedRpcRegistration instance to the
 * Blueprint container.
 *
 * @author Thomas Pantelis
 */
class RoutedRpcMetadata implements ComponentFactoryMetadata {
    private static final Logger LOG = LoggerFactory.getLogger(RoutedRpcMetadata.class);

    private final String id;
    private final String interfaceName;
    private final String implementationRefId;
    private ExtendedBlueprintContainer container;

    RoutedRpcMetadata(String id, String interfaceName, String implementationRefId) {
        this.id = id;
        this.interfaceName = interfaceName;
        this.implementationRefId = implementationRefId;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public int getActivation() {
        return ACTIVATION_LAZY;
    }

    @Override
    public List<String> getDependsOn() {
        return Arrays.asList(OpendaylightNamespaceHandler.RPC_REGISTRY_NAME, implementationRefId);
    }

    @Override
    public void init(ExtendedBlueprintContainer container) {
        this.container = container;

        LOG.debug("{}: In init", logName());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object create() throws ComponentDefinitionException {
        RpcProviderRegistry rpcRegistry = (RpcProviderRegistry) container.getComponentInstance(
                OpendaylightNamespaceHandler.RPC_REGISTRY_NAME);

        Object implementation = container.getComponentInstance(implementationRefId);

        LOG.debug("{}: create - adding routed implementation {} for rpc class {}", logName(),
                implementation, interfaceName);

        try {
            Preconditions.checkArgument(RpcService.class.isAssignableFrom(implementation.getClass()),
                    "Implementation ref instance %s is not an RpcService", implementation);

            Class<?> rpcInterface = container.getBundleContext().getBundle().loadClass(interfaceName);
            Preconditions.checkArgument(rpcInterface.isAssignableFrom(implementation.getClass()),
                    "Specified interface %s is not implemented by RpcService %s", interfaceName,
                    implementation.getClass());

            return rpcRegistry.addRoutedRpcImplementation((Class<RpcService>)rpcInterface,
                    (RpcService)implementation);
        } catch(Exception e) {
            throw new ComponentDefinitionException("Error adding routed RPC implementation " +
                    implementation.getClass(), e);
        }
    }

    @Override
    public void destroy(Object instance) {
        LOG.debug("{}: In destroy: instance: {}", logName(), instance);

        (( RoutedRpcRegistration<?>)instance).close();
    }

    private String logName() {
        return (container != null ? container.getBundleContext().getBundle().getSymbolicName() : "") +
                " (" + id + ")";
    }
}
