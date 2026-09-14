/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.access.client;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class InversibleLockTest {

    private InversibleLock lock;
    private ScheduledExecutorService executor;

    @Before
    public void setUp() {
        lock = new InversibleLock();
        executor = Executors.newScheduledThreadPool(1);
    }

    @After
    public void tearDown() {
        executor.shutdownNow();
    }

    @Test(timeout = 2000)
    public void testWriteLockUnlock() {
        final long stamp = lock.writeLock();
        Assert.assertTrue(lock.validate(stamp));
        executor.schedule(() -> lock.unlockWrite(stamp), 500, TimeUnit.MILLISECONDS);
        try {
            lock.optimisticRead();
        } catch (final InversibleLockException e) {
            e.awaitResolution();
        }
    }

    @Test
    public void testLockAfterRead() {
        final long readStamp = lock.optimisticRead();
        lock.writeLock();
        Assert.assertFalse(lock.validate(readStamp));
    }
}
