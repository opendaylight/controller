/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.blueprint.ext;

import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.osgi.framework.Bundle;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Blueprint bean corresponding to the "rpc-implementation" element that registers an RPC implementation with
 * the RpcProviderRegistry.
 *
 * @author Thomas Pantelis
 */
public class RpcImplementationBean {
    private static final Logger LOG = LoggerFactory.getLogger(RpcImplementationBean.class);
    static final String RPC_IMPLEMENTATION = "rpc-implementation";

    private final List<ObjectRegistration<RpcService>> rpcRegistrations = new ArrayList<>();
    private RpcProviderService rpcProvider = null;
    private Bundle bundle = null;
    private String interfaceName = null;
    private RpcService implementation = null;

    public void setRpcProvider(final RpcProviderService rpcProvider) {
        this.rpcProvider = rpcProvider;
    }

    public void setBundle(final Bundle bundle) {
        this.bundle = bundle;
    }

    public void setInterfaceName(final String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public void setImplementation(final RpcService implementation) {
        this.implementation = implementation;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public void init() {
        try {
            final var rpcInterfaces = getImplementedRpcServiceInterfaces(interfaceName,
                    implementation.getClass(), bundle, RPC_IMPLEMENTATION);

            LOG.debug("{}: init - adding implementation {} for RpcService interface(s) {}", bundle.getSymbolicName(),
                    implementation, rpcInterfaces);

            for (var rpcInterface : rpcInterfaces) {
                rpcRegistrations.add(rpcProvider.registerRpcImplementation(rpcInterface, implementation));
            }
        } catch (final ComponentDefinitionException e) {
            throw e;
        } catch (final Exception e) {
            throw new ComponentDefinitionException(String.format(
                    "Error processing \"%s\" for %s", RPC_IMPLEMENTATION, implementation.getClass()), e);
        }
    }

    public void destroy() {
        for (ObjectRegistration<RpcService> reg: rpcRegistrations) {
            reg.close();
        }
    }

    @SuppressWarnings("unchecked")
    static List<Class<RpcService>> getImplementedRpcServiceInterfaces(final String interfaceName,
            final Class<?> implementationClass, final Bundle bundle, final String logName)
            throws ClassNotFoundException {
        if (!Strings.isNullOrEmpty(interfaceName)) {
            Class<?> rpcInterface = bundle.loadClass(interfaceName);

            if (!rpcInterface.isAssignableFrom(implementationClass)) {
                throw new ComponentDefinitionException(String.format(
                        "The specified \"interface\" %s for \"%s\" is not implemented by RpcService \"ref\" %s",
                        interfaceName, logName, implementationClass));
            }

            return Collections.singletonList((Class<RpcService>)rpcInterface);
        }

        final var rpcInterfaces = new ArrayList<Class<RpcService>>();
        for (var intface : implementationClass.getInterfaces()) {
            if (RpcService.class.isAssignableFrom(intface)) {
                rpcInterfaces.add((Class<RpcService>) intface);
            }
        }

        if (rpcInterfaces.isEmpty()) {
            throw new ComponentDefinitionException(String.format(
                    "The \"ref\" instance %s for \"%s\" does not implemented any RpcService interfaces",
                    implementationClass, logName));
        }

        return rpcInterfaces;
    }
}
