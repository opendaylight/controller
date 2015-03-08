/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementationRegistration;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;
import org.opendaylight.yangtools.yang.binding.RpcService;

class BindingRpcAdapterRegistration<T extends RpcService> extends AbstractObjectRegistration<T>{

    private final DOMRpcImplementationRegistration<?> reg;

    public BindingRpcAdapterRegistration(T instance, DOMRpcImplementationRegistration<?> reg) {
        super(instance);
        this.reg = reg;
    }

    @Override
    protected void removeRegistration() {
        reg.close();
    }
}
