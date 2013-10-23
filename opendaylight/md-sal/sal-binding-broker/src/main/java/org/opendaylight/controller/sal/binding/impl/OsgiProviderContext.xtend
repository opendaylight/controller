/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.impl;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.osgi.framework.BundleContext;

import static org.opendaylight.controller.sal.binding.impl.osgi.Constants.*;
import static extension org.opendaylight.controller.sal.binding.impl.osgi.PropertiesUtils.*;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider.ProviderFunctionality
import static com.google.common.base.Preconditions.*

class OsgiProviderContext extends OsgiConsumerContext implements ProviderContext {

    @Property
    val Map<Class<? extends RpcService>, RpcRegistration<? extends RpcService>> registeredServices

    new(BundleContext ctx, BindingAwareBrokerImpl broker) {
        super(ctx, broker);
        _registeredServices = new HashMap();
    }

    override <T extends RpcService> addRpcImplementation(Class<T> type, T implementation) {

        // TODO Auto-generated method stub
        val properties = new Hashtable<String, String>();
        properties.salServiceType = SAL_SERVICE_TYPE_PROVIDER

        // Fill requirements
        val salReg = broker.registerRpcImplementation(type, implementation, this, properties)
        registeredServices.put(type, salReg)
        return salReg;
    }

    override <T extends RpcService> addRoutedRpcImplementation(Class<T> type, T implementation) throws IllegalStateException {
        checkNotNull(type, "Service type should not be null")
        checkNotNull(implementation, "Service type should not be null")
        
        val salReg = broker.registerRoutedRpcImplementation(type, implementation, this)
        registeredServices.put(type, salReg)
        return salReg;
    }

    override registerFunctionality(ProviderFunctionality functionality) {
        // NOOP for now
    }

    override unregisterFunctionality(ProviderFunctionality functionality) {
        // NOOP for now
    }
}
