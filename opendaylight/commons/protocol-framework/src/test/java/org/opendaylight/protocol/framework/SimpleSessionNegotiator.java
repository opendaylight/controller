/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import io.netty.channel.Channel;
import io.netty.util.concurrent.Promise;

public class SimpleSessionNegotiator extends AbstractSessionNegotiator<SimpleMessage, SimpleSession> {

    public SimpleSessionNegotiator(final Promise<SimpleSession> promise, final Channel channel) {
        super(promise, channel);
    }

    @Override
    protected void startNegotiation() throws Exception {
        negotiationSuccessful(new SimpleSession());
    }

    @Override
    protected void handleMessage(final SimpleMessage msg) throws Exception {
        throw new IllegalStateException("This method should never be invoked");
    }
}
