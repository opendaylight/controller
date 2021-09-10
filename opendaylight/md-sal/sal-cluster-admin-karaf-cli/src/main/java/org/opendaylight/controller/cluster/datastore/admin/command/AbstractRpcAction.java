/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.admin.command;


import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;
import org.apache.karaf.shell.api.action.Action;
import org.opendaylight.yangtools.yang.common.RpcResult;


/**
 * Common base class for all commands which end up invoking an RPC.
 */
public abstract class AbstractRpcAction implements Action {
    @Override
    @SuppressWarnings("checkstyle:RegexpSinglelineJava")
    public final Object execute() throws InterruptedException, ExecutionException {
        final RpcResult<?> result = invokeRpc().get();
        if (!result.isSuccessful()) {
            // FIXME: is there a better way to report errors?
            System.out.println("Invocation failed: " + result.getErrors());
        }
        return null;
    }

    protected abstract ListenableFuture<? extends RpcResult<?>> invokeRpc();
}
