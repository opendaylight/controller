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
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
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
abstract class AbstractInvokableImplementationBean {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractInvokableImplementationBean.class);

    private final List<RpcRegistration<RpcService>> rpcRegistrations = new ArrayList<>(1);
    private RpcProviderRegistry rpcRegistry;
    private RpcService implementation;
    private String interfaceName;
    private Bundle bundle;

    public void setRpcRegistry(final RpcProviderRegistry rpcRegistry) {
        this.rpcRegistry = rpcRegistry;
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

    abstract String logName();

    @SuppressWarnings("checkstyle:IllegalCatch")
    public void init() {
        final List<Class<RpcService>> rpcInterfaces;
        try {
            rpcInterfaces = getImplementedRpcServiceInterfaces(interfaceName, implementation.getClass(), bundle);
        } catch (ClassNotFoundException e) {
            throw new ComponentDefinitionException(String.format("Error processing \"%s\" for %s", logName(),
                implementation.getClass()), e);
        }

        LOG.debug("{}: init - adding implementation {} for RpcService interface(s) {}", bundle.getSymbolicName(),
            implementation, rpcInterfaces);

        for (Class<RpcService> rpcInterface : rpcInterfaces) {
            rpcRegistrations.add(rpcRegistry.addRpcImplementation(rpcInterface, implementation));
        }
    }

    public void destroy() {
        for (RpcRegistration<RpcService> reg: rpcRegistrations) {
            reg.close();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Class<RpcService>> getImplementedRpcServiceInterfaces(final String interfaceName,
            final Class<?> implementationClass, final Bundle bundle) throws ClassNotFoundException {
        if (!Strings.isNullOrEmpty(interfaceName)) {
            Class<?> rpcInterface = bundle.loadClass(interfaceName);

            if (!rpcInterface.isAssignableFrom(implementationClass)) {
                throw new ComponentDefinitionException(String.format(
                        "The specified \"interface\" %s for \"%s\" is not implemented by RpcService \"ref\" %s",
                        interfaceName, logName(), implementationClass));
            }

            return Collections.singletonList((Class<RpcService>)rpcInterface);
        }

        List<Class<RpcService>> rpcInterfaces = new ArrayList<>();
        for (Class<?> intface : implementationClass.getInterfaces()) {
            if (RpcService.class.isAssignableFrom(intface)) {
                rpcInterfaces.add((Class<RpcService>) intface);
            }
        }

        if (rpcInterfaces.isEmpty()) {
            throw new ComponentDefinitionException(String.format(
                    "The \"ref\" instance %s for \"%s\" does not implemented any RpcService interfaces",
                    implementationClass, logName()));
        }

        return rpcInterfaces;
    }
}
