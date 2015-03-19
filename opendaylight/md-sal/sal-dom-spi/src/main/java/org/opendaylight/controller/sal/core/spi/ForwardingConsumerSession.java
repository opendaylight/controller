/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.spi;

import org.opendaylight.controller.sal.core.api.Broker.ConsumerSession;
import org.opendaylight.controller.sal.core.api.BrokerService;

public abstract class ForwardingConsumerSession implements ConsumerSession {

    protected abstract ConsumerSession delegate();

    @Override
    public void close() {
        delegate().close();
    }

    @Override
    public <T extends BrokerService> T getService(Class<T> arg0) {
        return delegate().getService(arg0);
    }

    @Override
    public boolean isClosed() {
        return delegate().isClosed();
    }

}
