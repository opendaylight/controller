/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.core.spi;

import java.util.Set;
import java.util.concurrent.Future;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.Broker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.core.api.Broker.RpcRegistration;
import org.opendaylight.controller.sal.core.api.BrokerService;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.controller.sal.core.api.RpcRegistrationListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;

public abstract class ForwardingProviderSession implements ProviderSession {


    protected abstract ProviderSession delegate();

    @Override
    @Deprecated
    public RoutedRpcRegistration addMountedRpcImplementation(QName arg0, RpcImplementation arg1) {
        return delegate().addMountedRpcImplementation(arg0, arg1);
    }

    @Override
    @Deprecated
    public RoutedRpcRegistration addRoutedRpcImplementation(QName arg0, RpcImplementation arg1) {
        return delegate().addRoutedRpcImplementation(arg0, arg1);
    }

    @Override
    @Deprecated
    public RpcRegistration addRpcImplementation(QName arg0, RpcImplementation arg1)
            throws IllegalArgumentException {
        return delegate().addRpcImplementation(arg0, arg1);
    }

    @Deprecated
    @Override
    public ListenerRegistration<RpcRegistrationListener> addRpcRegistrationListener(
            RpcRegistrationListener arg0) {
        return delegate().addRpcRegistrationListener(arg0);
    }

    @Override
    public void close() {
        delegate().close();
    }

    @Override
    public <T extends BrokerService> T getService(Class<T> arg0) {
        return delegate().getService(arg0);
    }

    @Override
    public Set<QName> getSupportedRpcs() {
        return delegate().getSupportedRpcs();
    }

    @Override
    public boolean isClosed() {
        return delegate().isClosed();
    }

    @Override
    public Future<RpcResult<CompositeNode>> rpc(QName arg0, CompositeNode arg1) {
        return delegate().rpc(arg0, arg1);
    }

}
