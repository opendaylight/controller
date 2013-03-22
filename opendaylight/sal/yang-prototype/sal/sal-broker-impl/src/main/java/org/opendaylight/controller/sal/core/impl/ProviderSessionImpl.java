
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.impl;

import java.util.Set;

import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.yang.common.QName;


public class ProviderSessionImpl extends ConsumerSessionImpl implements
        ProviderSession {

    private Provider provider;

    public ProviderSessionImpl(BrokerImpl broker, Provider provider) {
        super(broker, null);
        this.provider = provider;
    }

    @Override
    public void addRpcImplementation(QName rpcType,
            RpcImplementation implementation) throws IllegalArgumentException {
        // TODO Implement this method
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void removeRpcImplementation(QName rpcType,
            RpcImplementation implementation) throws IllegalArgumentException {
        // TODO Implement this method
        throw new UnsupportedOperationException("Not implemented");
    }

    public Provider getProvider() {
        return this.provider;
    }

}

