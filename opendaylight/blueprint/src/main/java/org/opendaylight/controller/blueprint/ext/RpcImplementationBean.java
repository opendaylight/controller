/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.blueprint.ext;

import com.google.common.base.Preconditions;
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
public class RpcImplementationBean {
    private static final Logger LOG = LoggerFactory.getLogger(RpcImplementationBean.class);

    private RpcProviderRegistry rpcRegistry;
    private Bundle bundle;
    private String interfaceName;
    private RpcService implementation;
    private final List<RpcRegistration<RpcService>> rpcRegistrations = new ArrayList<>();;

    public void setRpcRegistry(RpcProviderRegistry rpcRegistry) {
        this.rpcRegistry = rpcRegistry;
    }

    public void setBundle(Bundle bundle) {
        this.bundle = bundle;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public void setImplementation(RpcService implementation) {
        this.implementation = implementation;
    }

    public void init() {
        try {
            List<Class<RpcService>> rpcInterfaces = getImplementedRpcServiceInterfaces(interfaceName,
                    implementation.getClass(), bundle);

            LOG.debug("{}: init - adding implementation {} for RpcService interface(s) {}", bundle.getSymbolicName(),
                    implementation, rpcInterfaces);

            for(Class<RpcService> rpcInterface: rpcInterfaces) {
                rpcRegistrations.add(rpcRegistry.addRpcImplementation(rpcInterface, implementation));
            }
        } catch(Exception e) {
            throw new ComponentDefinitionException("Error adding RPC implementation " + implementation.getClass(), e);
        }
    }

    public void destroy() {
        for(RpcRegistration<RpcService> reg: rpcRegistrations) {
            reg.close();
        }
    }

    @SuppressWarnings("unchecked")
    static List<Class<RpcService>> getImplementedRpcServiceInterfaces(String interfaceName,
            Class<?> implementationClass, Bundle bundle) throws ClassNotFoundException {
        if(!Strings.isNullOrEmpty(interfaceName)) {
            Class<?> rpcInterface = bundle.loadClass(interfaceName);
            Preconditions.checkArgument(rpcInterface.isAssignableFrom(implementationClass),
                    "Specified interface %s is not implemented by RpcService %s", interfaceName, implementationClass);
            return Collections.singletonList((Class<RpcService>)rpcInterface);
        }

        List<Class<RpcService>> rpcInterfaces = new ArrayList<>();
        for(Class<?> intface: implementationClass.getInterfaces()) {
            if(RpcService.class.isAssignableFrom(intface)) {
                rpcInterfaces.add((Class<RpcService>) intface);
            }
        }

        return rpcInterfaces;
    }
}
