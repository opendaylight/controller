/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker.impl;

import org.opendaylight.controller.sal.core.api.Broker.RpcRegistration;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;
import org.opendaylight.yangtools.yang.common.QName;

class GlobalRpcRegistration extends AbstractObjectRegistration<RpcImplementation> implements
        RpcRegistration {
    private final QName type;
    private SchemaAwareRpcBroker router;

    public GlobalRpcRegistration(final QName type, final RpcImplementation instance, final SchemaAwareRpcBroker router) {
        super(instance);
        this.type = type;
        this.router = router;
    }

    @Override
    public QName getType() {
        return type;
    }

    @Override
    protected void removeRegistration() {
        if (router != null) {
            router.remove(this);
            router = null;
        }
    }
}