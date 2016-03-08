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

public class NioEventLoopGroupCloseable extends NioEventLoopGroup implements AutoCloseable {
    private NioEventLoopGroupCloseable(int threadCount) {
        super(threadCount);
    }

    private NioEventLoopGroupCloseable() {
        super();
    }

    @Override
    public void close() throws Exception {
        shutdownGracefully(0, 1, TimeUnit.SECONDS);
    }

    public static NioEventLoopGroupCloseable newInstance(Integer threadCount) {
        if(threadCount == null || threadCount <= 0) {
            return new NioEventLoopGroupCloseable();
        }

        return new NioEventLoopGroupCloseable(threadCount);
    }
}