/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.blueprint.ext;

import com.google.common.base.Preconditions;
import java.util.Collections;
import java.util.List;
import org.apache.aries.blueprint.ext.ComponentFactoryMetadata;
import org.apache.aries.blueprint.services.ExtendedBlueprintContainer;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory metadata corresponding to the "rpc-service" element that gets an RPC service implementation from
 * the RpcProviderRegistry and provides it to the Blueprint container.
 *
 * @author Thomas Pantelis
 */
class RpcServiceMetadata implements ComponentFactoryMetadata {
    private static final Logger LOG = LoggerFactory.getLogger(RpcServiceMetadata.class);

    private final String id;
    private final String interfaceName;
    private ExtendedBlueprintContainer container;

    RpcServiceMetadata(String id, String interfaceName) {
        this.id = id;
        this.interfaceName = interfaceName;
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
        return Collections.singletonList(OpendaylightNamespaceHandler.RPC_REGISTRY_NAME);
    }

    @Override
    public void init(ExtendedBlueprintContainer container) {
        this.container = container;

        LOG.debug("{}: In init", logName());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object create() throws ComponentDefinitionException {
        LOG.debug("{}: In create: interfaceName: {}", logName(), interfaceName);

        RpcProviderRegistry rpcRegistry = (RpcProviderRegistry) container.getComponentInstance(
                OpendaylightNamespaceHandler.RPC_REGISTRY_NAME);

        try {
            Class<?> rpcInterface = container.getBundleContext().getBundle().loadClass(interfaceName);
            Preconditions.checkArgument(RpcService.class.isAssignableFrom(rpcInterface),
                    "Specified interface %s is not an RpcService", interfaceName);

            RpcService rpcService = rpcRegistry.getRpcService((Class<RpcService>)rpcInterface);

            LOG.debug("{}: create returning service {}", logName(), rpcService);

            return rpcService;
        } catch(Exception e) {
            throw new ComponentDefinitionException("Error getting RPC service for " + interfaceName, e);
        }
    }

    @Override
    public void destroy(Object instance) {
    }

    private String logName() {
        return (container != null ? container.getBundleContext().getBundle().getSymbolicName() : "") +
                " (" + id + ")";
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("RpcServiceMetadata [id=").append(id).append(", interfaceName=").append(interfaceName)
                .append("]");
        return builder.toString();
    }
}
