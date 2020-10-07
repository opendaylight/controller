/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.blueprint.ext;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Set;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.dom.api.DOMRpcIdentifier;
import org.opendaylight.mdsal.dom.api.DOMRpcImplementationNotAvailableException;
import org.opendaylight.mdsal.dom.api.DOMRpcProviderService;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.dom.spi.RpcRoutingStrategy;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.yangtools.yang.common.QName;
import org.osgi.framework.Bundle;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Blueprint bean corresponding to the "action-provider" element that registers the promise to instantiate action
 * instances with RpcProviderRegistry.
 *
 * <p>
 * This bean has two distinct facets:
 * - if a reference bean is provided, it registers it with {@link RpcProviderService}
 * - if a reference bean is not provided, it registers the corresponding no-op implementation with
 *   {@link DOMRpcProviderService} for all action (Routed RPC) elements in the provided interface
 *
 * @author Robert Varga
 */
public class ActionProviderBean {
    static final String ACTION_PROVIDER = "action-provider";

    private static final Logger LOG = LoggerFactory.getLogger(ActionProviderBean.class);

    private DOMRpcProviderService domRpcProvider;
    private RpcProviderService bindingRpcProvider;
    private DOMSchemaService schemaService;
    private RpcService implementation;
    private String interfaceName;
    private Registration reg;
    private Bundle bundle;

    public void setBundle(final Bundle bundle) {
        this.bundle = bundle;
    }

    public void setInterfaceName(final String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public void setImplementation(final RpcService implementation) {
        this.implementation = implementation;
    }

    public void setDomRpcProvider(final DOMRpcProviderService rpcProviderService) {
        this.domRpcProvider = rpcProviderService;
    }

    public void setBindingRpcProvider(final RpcProviderService rpcProvider) {
        this.bindingRpcProvider = rpcProvider;
    }

    public void setSchemaService(final DOMSchemaService schemaService) {
        this.schemaService = schemaService;
    }

    public void init() {
        // First resolve the interface class
        final Class<RpcService> interfaceClass = getRpcClass();

        LOG.debug("{}: resolved interface {} to {}", ACTION_PROVIDER, interfaceName, interfaceClass);

        if (implementation != null) {
            registerImplementation(interfaceClass);
        } else {
            registerFallback(interfaceClass);
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public void destroy() {
        if (reg != null) {
            try {
                reg.close();
            } catch (final Exception e) {
                LOG.warn("{}: error while unregistering", ACTION_PROVIDER, e);
            } finally {
                reg = null;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Class<RpcService> getRpcClass() {
        final Class<?> iface;

        try {
            iface = bundle.loadClass(interfaceName);
        } catch (final ClassNotFoundException e) {
            throw new ComponentDefinitionException(String.format(
                "The specified \"interface\" for %s \"%s\" does not refer to an available class", interfaceName,
                ACTION_PROVIDER), e);
        }
        if (!RpcService.class.isAssignableFrom(iface)) {
            throw new ComponentDefinitionException(String.format(
                "The specified \"interface\" %s for \"%s\" is not an RpcService", interfaceName, ACTION_PROVIDER));
        }

        return (Class<RpcService>) iface;
    }

    private void registerFallback(final Class<RpcService> interfaceClass) {
        final Collection<QName> paths = RpcUtil.decomposeRpcService(interfaceClass,
            schemaService.getGlobalContext(), RpcRoutingStrategy::isContextBasedRouted);
        if (paths.isEmpty()) {
            LOG.warn("{}: interface {} has no actions defined", ACTION_PROVIDER, interfaceClass);
            return;
        }

        final Set<DOMRpcIdentifier> rpcs = ImmutableSet.copyOf(Collections2.transform(paths, DOMRpcIdentifier::create));
        reg = domRpcProvider.registerRpcImplementation(
            (rpc, input) -> FluentFutures.immediateFailedFluentFuture(new DOMRpcImplementationNotAvailableException(
                "Action %s has no instance matching %s", rpc, input)), rpcs);
        LOG.debug("Registered provider for {}", interfaceName);
    }

    private void registerImplementation(final Class<RpcService> interfaceClass) {
        if (!interfaceClass.isInstance(implementation)) {
            throw new ComponentDefinitionException(String.format(
                "The specified \"interface\" %s for \"%s\" is not implemented by RpcService \"ref\" %s",
                interfaceName, ACTION_PROVIDER, implementation.getClass()));
        }

        reg = bindingRpcProvider.registerRpcImplementation(interfaceClass, implementation);
        LOG.debug("Registered implementation {} for {}", implementation, interfaceName);
    }
}
