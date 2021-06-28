/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.netty.threadgroup;

import io.netty.channel.nio.NioEventLoopGroup;
import java.util.concurrent.TimeUnit;

abstract class AbstractGlobalGroup extends NioEventLoopGroup implements AutoCloseable {
    AbstractGlobalGroup(final int threadCount) {
        super(threadCount < 0 ? 0 : threadCount);
    }

    @Override
    public final void close() {
        shutdownGracefully(0, 1, TimeUnit.SECONDS);
    }
}
