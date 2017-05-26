/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.impl.service;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public abstract class AbstractDataTransaction {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractDataTransaction.class);
    private static final ListenableFuture<RpcResult<TransactionStatus>> SUCCESS_FUTURE =
            Futures.immediateFuture(RpcResultBuilder.success(TransactionStatus.COMMITED).build());

    public static ListenableFuture<RpcResult<TransactionStatus>> convertToLegacyCommitFuture(final CheckedFuture<Void,TransactionCommitFailedException> from) {
        return Futures.transform(from, new AsyncFunction<Void, RpcResult<TransactionStatus>>() {
            @Override
            public ListenableFuture<RpcResult<TransactionStatus>> apply(final Void input) {
                return SUCCESS_FUTURE;
            }
        });
    }
}
