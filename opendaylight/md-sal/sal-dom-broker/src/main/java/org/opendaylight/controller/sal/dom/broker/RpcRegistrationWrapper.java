/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.sal.core.api.Broker.RpcRegistration;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.yangtools.yang.common.QName;

public class RpcRegistrationWrapper implements RpcRegistration {

    private final RpcRegistration delegate;

    public RpcRegistrationWrapper(final RpcRegistration delegate) {
        this.delegate = Preconditions.checkNotNull(delegate);
    }

    @Override
    public RpcImplementation getInstance() {
        return delegate.getInstance();
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public QName getType() {
        return delegate.getType();
    }

    /**
     * @return the delegate
     */
    public RpcRegistration getDelegate() {
        return delegate;
    }
}