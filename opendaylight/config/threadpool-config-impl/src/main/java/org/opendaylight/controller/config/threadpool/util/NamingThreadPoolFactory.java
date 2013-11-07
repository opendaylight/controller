/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.threadpool.util;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.concurrent.ThreadSafe;

import com.google.common.base.Preconditions;

/**
 * Implementation of {@link ThreadFactory}.
 */
@ThreadSafe
public class NamingThreadPoolFactory implements ThreadFactory, Closeable {

    private final ThreadGroup group;
    private final String namePrefix;
    private final AtomicLong threadName = new AtomicLong();

    public NamingThreadPoolFactory(String namePrefix) {
        Preconditions.checkNotNull(namePrefix);
        this.group = new ThreadGroup(namePrefix);
        this.namePrefix = namePrefix;
    }

    @Override
    public Thread newThread(Runnable r) {
        return new Thread(group, r, String.format("%s-%d", group.getName(), threadName.incrementAndGet()));
    }

    @Override
    public void close() throws IOException {
    }

    public String getNamePrefix() {
        return namePrefix;
    }

}
