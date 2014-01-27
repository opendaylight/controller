/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.codegen.impl;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import org.opendaylight.controller.sal.binding.codegen.RuntimeCodeGenerator;
import org.opendaylight.controller.sal.binding.spi.NotificationInvokerFactory;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import javassist.ClassPool;

public class SingletonHolder {

    public static final ClassPool CLASS_POOL = new ClassPool();
    public static final org.opendaylight.controller.sal.binding.codegen.impl.RuntimeCodeGenerator RPC_GENERATOR_IMPL = new org.opendaylight.controller.sal.binding.codegen.impl.RuntimeCodeGenerator(
            CLASS_POOL);
    public static final RuntimeCodeGenerator RPC_GENERATOR = RPC_GENERATOR_IMPL;
    public static final NotificationInvokerFactory INVOKER_FACTORY = RPC_GENERATOR_IMPL.getInvokerFactory();
    private static ListeningExecutorService NOTIFICATION_EXECUTOR = null;
    private static ListeningExecutorService COMMIT_EXECUTOR = null;

    public static synchronized final ListeningExecutorService getDefaultNotificationExecutor() {
        if (NOTIFICATION_EXECUTOR == null) {
            NOTIFICATION_EXECUTOR = createNamedExecutor("md-sal-binding-notification-%d");
        }
        return NOTIFICATION_EXECUTOR;
    }

    public static synchronized final ListeningExecutorService getDefaultCommitExecutor() {
        if (COMMIT_EXECUTOR == null) {
            COMMIT_EXECUTOR = createNamedExecutor("md-sal-binding-commit-%d");
        }

        return COMMIT_EXECUTOR;
    }

    private static ListeningExecutorService createNamedExecutor(String format) {
        ThreadFactory factory = new ThreadFactoryBuilder().setDaemon(true).setNameFormat(format).build();
        ExecutorService executor = Executors.newCachedThreadPool(factory);
        return MoreExecutors.listeningDecorator(executor);

    }

}
