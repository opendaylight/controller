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
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcServiceRegistration;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.osgi.framework.BundleContext;

import static extension org.opendaylight.controller.sal.binding.impl.utils.PropertiesUtils.*;

class OsgiProviderContext extends OsgiConsumerContext implements ProviderContext {

    @Property
    val Map<Class<? extends RpcService>, RpcServiceRegistrationImpl<? extends RpcService>> registeredServices

    new(BundleContext ctx, BindingAwareBrokerImpl broker) {
        super(ctx, broker);
        _registeredServices = new HashMap();
    }

    override def <T extends RpcService> RpcServiceRegistration<T> addRpcImplementation(Class<T> type, T implementation) {

        // TODO Auto-generated method stub
        val properties = new Hashtable<String, String>();
        properties.salServiceType = Constants.SAL_SERVICE_TYPE_PROVIDER

        // Fill requirements
        val salReg = broker.registerRpcImplementation(type, implementation, this, properties)
        registeredServices.put(type, salReg)
        return salReg;
    }
}
