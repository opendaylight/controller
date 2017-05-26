/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.threadpool.scheduled;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.util.concurrent.ListenableFutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.management.ObjectName;
import org.opendaylight.controller.config.api.DynamicMBeanWithInstance;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.manager.impl.AbstractMockedModule;
import org.opendaylight.controller.config.spi.Module;
import org.opendaylight.controller.config.threadpool.util.ScheduledThreadPoolWrapper;
import org.opendaylight.controller.config.yang.threadpool.ScheduledThreadPoolServiceInterface;
import org.opendaylight.controller.config.yang.threadpool.impl.scheduled.ScheduledThreadPoolModuleMXBean;

public class TestingScheduledThreadPoolModule extends AbstractMockedModule implements
        ScheduledThreadPoolServiceInterface, Module, ScheduledThreadPoolModuleMXBean {

    public TestingScheduledThreadPoolModule(DynamicMBeanWithInstance old, ModuleIdentifier id) {
        super(old, id);
    }

    @Override
    protected AutoCloseable prepareMockedInstance() throws Exception {
        ScheduledThreadPoolWrapper instance = mock(ScheduledThreadPoolWrapper.class);
        ScheduledExecutorService ses = mock(ScheduledExecutorService.class);
        {// mockFuture
            ScheduledFuture<?> future = mock(ScheduledFuture.class);
            doReturn(false).when(future).cancel(anyBoolean());
            try {
                doReturn(mock(Object.class)).when(future).get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            doReturn(future).when(ses).schedule(any(Runnable.class), any(Long.class), any(TimeUnit.class));
            doReturn(future).when(ses).scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(),
                    any(TimeUnit.class));

        }
        doNothing().when(ses).execute(any(Runnable.class));
        doNothing().when(ses).execute(any(ListenableFutureTask.class));
        doReturn(ses).when(instance).getExecutor();
        doNothing().when(instance).close();

        doReturn(1).when(instance).getMaxThreadCount();
        return instance;
    }

    @Override
    public ObjectName getThreadFactory() {
        return any(ObjectName.class);
    }

    @Override
    public void setThreadFactory(ObjectName threadFactory) {
    }

    @Override
    public Integer getMaxThreadCount() {
        return 1;
    }

    @Override
    public void setMaxThreadCount(Integer maxThreadCount) {
    }

}
