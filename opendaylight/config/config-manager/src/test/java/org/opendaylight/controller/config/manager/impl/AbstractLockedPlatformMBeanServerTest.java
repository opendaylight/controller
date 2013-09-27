/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl;

import java.lang.management.ManagementFactory;
import java.util.concurrent.locks.ReentrantLock;

import javax.management.MBeanServer;

import org.junit.After;
import org.junit.Before;

/**
 * Each test that works with platform MBeanServer should extend this class.
 */
public abstract class AbstractLockedPlatformMBeanServerTest {
    private static final ReentrantLock lock = new ReentrantLock();
    protected static MBeanServer platformMBeanServer = ManagementFactory
            .getPlatformMBeanServer();

    @Before
    public void acquireLock() {
        lock.lock();
    }

    @After
    public void unlock() {
        lock.unlock();
    }

    public static class SimpleBean implements SimpleBeanMBean {

    }

    public static interface SimpleBeanMBean {

    }

}
