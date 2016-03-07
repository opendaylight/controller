/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.api;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper class for RpcProviderRegistry for use via blueprint XML where Class instances can't be referenced.
 * @author Thomas Pantelis
 */
public class RpcProviderRegistryWrapper {
    private static final Logger LOG = LoggerFactory.getLogger(RpcProviderRegistryWrapper.class);

    private final RpcProviderRegistry rpcRegistry;
    private final BundleContext bundleContext;

    public RpcProviderRegistryWrapper(RpcProviderRegistry rpcRegistry, BundleContext bundleContext) {
        this.rpcRegistry = rpcRegistry;
        this.bundleContext = bundleContext;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public RoutedRpcRegistration addRoutedRpcImplementation(String className, RpcService implementation) {
        try {
            return rpcRegistry.addRoutedRpcImplementation((Class)bundleContext.getBundle().loadClass(className),
                    implementation);
        } catch (Exception e) {
            LOG.error("Error registering routed RPC implementation for class " + className, e);
            throw new IllegalArgumentException("Error registering routed RPC implementation for class " + className, e);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public RpcRegistration addRpcImplementation(String className, RpcService implementation) {
        try {
            return rpcRegistry.addRpcImplementation((Class)bundleContext.getBundle().loadClass(className),
                    implementation);
        } catch (Exception e) {
            LOG.error("Error registering global RPC implementation for class " + className, e);
            throw new IllegalArgumentException("Error registering global RPC implementation for class " + className, e);
        }
    }
}