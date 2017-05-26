/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.test.util;

import java.util.concurrent.ExecutorService;

import javassist.ClassPool;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

@Beta
public class BindingBrokerTestFactory {

    private static final ClassPool CLASS_POOL = ClassPool.getDefault();
    private boolean startWithParsedSchema = true;
    private ExecutorService executor;
    private ClassPool classPool;


    public boolean isStartWithParsedSchema() {
        return startWithParsedSchema;
    }

    public void setStartWithParsedSchema(final boolean startWithParsedSchema) {
        this.startWithParsedSchema = startWithParsedSchema;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public void setExecutor(final ExecutorService executor) {
        this.executor = executor;
    }


    public BindingTestContext getTestContext() {
        Preconditions.checkState(executor != null, "Executor is not set.");
        ListeningExecutorService listenableExecutor = MoreExecutors.listeningDecorator(executor);
        return new BindingTestContext(listenableExecutor, getClassPool(),startWithParsedSchema);
    }

    public ClassPool getClassPool() {
        if(classPool == null) {
            return CLASS_POOL;
        }

        return classPool;
    }

    public void setClassPool(final ClassPool classPool) {
        this.classPool = classPool;
    }

}
