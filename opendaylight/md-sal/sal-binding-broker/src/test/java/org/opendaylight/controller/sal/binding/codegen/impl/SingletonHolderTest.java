/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.binding.codegen.impl;

import com.google.common.util.concurrent.ListeningExecutorService;
import java.lang.reflect.Field;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Ignore
public class SingletonHolderTest {
    private static final Logger logger = LoggerFactory.getLogger(SingletonHolderTest.class);

    @Test
    public void testNotificationExecutor() throws Exception {
        ListeningExecutorService executor = SingletonHolder.getDefaultNotificationExecutor();
        ThreadPoolExecutor tpExecutor = (ThreadPoolExecutor) setAccessible(executor.getClass().getDeclaredField("delegate")).get(executor);
        BlockingQueue<Runnable> queue = tpExecutor.getQueue();

        for (int idx = 0; idx < 100; idx++) {
            final int idx2 = idx;
            logger.info("Adding {}\t{}\t{}", idx, queue.size(), tpExecutor.getActiveCount());
            executor.execute(new Runnable() {

                @Override
                public void run() {
                    logger.info("in  {}", idx2);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    logger.info("out {}", idx2);
                }
            });
        }
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
    }

    private static Field setAccessible(Field field) {
        field.setAccessible(true);
        return field;
    }
}
