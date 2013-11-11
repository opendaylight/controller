/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util;

import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.Promise;
import org.opendaylight.controller.netconf.api.NetconfSession;

public abstract class AbstractChannelInitializer {

    public abstract void initialize(SocketChannel ch, Promise<? extends NetconfSession> promise);

    protected abstract void initializeAfterDecoder(SocketChannel ch, Promise<? extends NetconfSession> promise);

}
