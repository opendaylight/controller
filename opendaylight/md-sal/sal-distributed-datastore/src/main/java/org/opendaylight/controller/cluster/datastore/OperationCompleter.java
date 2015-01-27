/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.dispatch.OnComplete;
import com.google.common.base.Preconditions;
import java.util.concurrent.Semaphore;

final class OperationCompleter extends OnComplete<Object> {
    private final Semaphore operationLimiter;

    OperationCompleter(Semaphore operationLimiter){
        this.operationLimiter = Preconditions.checkNotNull(operationLimiter);
    }

    @Override
    public void onComplete(Throwable throwable, Object o){
        this.operationLimiter.release();
    }
}