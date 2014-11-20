/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.impl.service;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.common.impl.AbstractDataModification;
import org.opendaylight.yangtools.concepts.Path;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public abstract class AbstractDataTransaction<P extends Path<P>, D extends Object> extends
        AbstractDataModification<P, D> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractDataTransaction.class);
    private static final ListenableFuture<RpcResult<TransactionStatus>> SUCCESS_FUTURE =
            Futures.immediateFuture(RpcResultBuilder.success(TransactionStatus.COMMITED).build());

    private final Object identifier;
    private final long allocationTime;
    private long readyTime = 0;
    private long completeTime = 0;

    private TransactionStatus status = TransactionStatus.NEW;

    private final AbstractDataBroker<P, D, ? extends Object> broker;

    protected AbstractDataTransaction(final Object identifier,
            final AbstractDataBroker<P, D, ? extends Object> dataBroker) {
        super(dataBroker);
        this.identifier = Preconditions.checkNotNull(identifier);
        this.broker = Preconditions.checkNotNull(dataBroker);
        this.allocationTime = System.nanoTime();
        LOG.debug("Transaction {} Allocated.", identifier);
    }

    @Override
    public Object getIdentifier() {
        return this.identifier;
    }

    @Override
    public Future<RpcResult<TransactionStatus>> commit() {
        readyTime = System.nanoTime();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Transaction {} Ready after {}ms.", identifier, TimeUnit.NANOSECONDS.toMillis(readyTime - allocationTime));
        }
        changeStatus(TransactionStatus.SUBMITED);
        return this.broker.commit(this);
    }

    @Override
    public D readConfigurationData(final P path) {
        final D local = getUpdatedConfigurationData().get(path);
        if (local != null) {
            return local;
        }
        return this.broker.readConfigurationData(path);
    }

    @Override
    public D readOperationalData(final P path) {
        final D local = this.getUpdatedOperationalData().get(path);
        if (local != null) {
            return local;
        }
        return this.broker.readOperationalData(path);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((identifier == null) ? 0 : identifier.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AbstractDataTransaction<?, ?> other = (AbstractDataTransaction<?, ?>) obj;
        if (identifier == null) {
            if (other.identifier != null) {
                return false;
            }
        } else if (!identifier.equals(other.identifier)) {
            return false;
        }
        return true;
    }

    @Override
    public TransactionStatus getStatus() {
        return this.status;
    }

    protected abstract void onStatusChange(final TransactionStatus status);

    public void succeeded() {
        this.completeTime = System.nanoTime();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Transaction {} Committed after {}ms.", identifier, TimeUnit.NANOSECONDS.toMillis(completeTime - readyTime));
        }
        changeStatus(TransactionStatus.COMMITED);
    }

    public void failed() {
        this.completeTime = System.nanoTime();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Transaction {} Failed after {}ms.", identifier, TimeUnit.NANOSECONDS.toMillis(completeTime - readyTime));
        }
        changeStatus(TransactionStatus.FAILED);
    }

    private void changeStatus(final TransactionStatus status) {
        LOG.debug("Transaction {} transitioned from {} to {}", getIdentifier(), this.status, status);
        this.status = status;
        this.onStatusChange(status);
    }

    public static ListenableFuture<RpcResult<TransactionStatus>> convertToLegacyCommitFuture(final CheckedFuture<Void,TransactionCommitFailedException> from) {
        return Futures.transform(from, new AsyncFunction<Void, RpcResult<TransactionStatus>>() {
            @Override
            public ListenableFuture<RpcResult<TransactionStatus>> apply(final Void input) {
                return SUCCESS_FUTURE;
            }
        });
    }
}
