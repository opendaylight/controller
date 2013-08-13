/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.impl

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcServiceRegistration
import org.osgi.framework.ServiceRegistration
import org.opendaylight.yangtools.yang.binding.RpcService

class RpcServiceRegistrationImpl<T extends RpcService> implements RpcServiceRegistration<T> {

    val ServiceRegistration<T> osgiRegistration;
    private val T service;
    val Class<T> cls;

    public new(Class<T> type, T service, ServiceRegistration<T> osgiReg) {
        this.cls = type;
        this.osgiRegistration = osgiReg;
        this.service = service;
    }

    override getService() {
        this.service
    }

    override unregister() {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }
}
