/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.blueprint.ext;

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
    static final String ROUTED_RPC_IMPLEMENTATION = "routed-rpc-implementation";

    private final String id;
    private final String interfaceName;
    private final String implementationRefId;
    private ExtendedBlueprintContainer container;

    RoutedRpcMetadata(final String id, final String interfaceName, final String implementationRefId) {
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
    public void init(final ExtendedBlueprintContainer newContainer) {
        this.container = newContainer;

        LOG.debug("{}: In init", logName());
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    @Override
    public Object create() throws ComponentDefinitionException {
        RpcProviderRegistry rpcRegistry = (RpcProviderRegistry) container.getComponentInstance(
                OpendaylightNamespaceHandler.RPC_REGISTRY_NAME);

        Object implementation = container.getComponentInstance(implementationRefId);

        try {
            if (!RpcService.class.isAssignableFrom(implementation.getClass())) {
                throw new ComponentDefinitionException(String.format(
                        "Implementation \"ref\" instance %s for \"%s\" is not an RpcService",
                        implementation.getClass(), ROUTED_RPC_IMPLEMENTATION));
            }

            List<Class<RpcService>> rpcInterfaces = RpcImplementationBean.getImplementedRpcServiceInterfaces(
                    interfaceName, implementation.getClass(), container.getBundleContext().getBundle(),
                    ROUTED_RPC_IMPLEMENTATION);

            if (rpcInterfaces.size() > 1) {
                throw new ComponentDefinitionException(String.format(
                        "Implementation \"ref\" instance %s for \"%s\" implements more than one RpcService "
                        + "interface (%s). Please specify the exact \"interface\"", implementation.getClass(),
                        ROUTED_RPC_IMPLEMENTATION, rpcInterfaces));
            }

            Class<RpcService> rpcInterface = rpcInterfaces.iterator().next();

            LOG.debug("{}: create - adding routed implementation {} for RpcService {}", logName(),
                    implementation, rpcInterface);

            return rpcRegistry.addRoutedRpcImplementation(rpcInterface, (RpcService)implementation);
        } catch (final ComponentDefinitionException e) {
            throw e;
        } catch (final Exception e) {
            throw new ComponentDefinitionException(String.format(
                    "Error processing \"%s\" for %s", ROUTED_RPC_IMPLEMENTATION, implementation.getClass()), e);
        }
    }

    @Override
    public void destroy(final Object instance) {
        LOG.debug("{}: In destroy: instance: {}", logName(), instance);

        ((RoutedRpcRegistration<?>)instance).close();
    }

    private String logName() {
        return (container != null ? container.getBundleContext().getBundle().getSymbolicName() : "") + " (" + id + ")";
    }
}
