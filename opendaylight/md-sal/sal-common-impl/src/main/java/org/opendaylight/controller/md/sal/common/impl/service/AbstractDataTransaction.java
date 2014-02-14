/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.impl.service;

import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.impl.AbstractDataModification;
import org.opendaylight.yangtools.concepts.Path;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("all")
public abstract class AbstractDataTransaction<P extends Path<P>, D extends Object> extends
        AbstractDataModification<P, D> {
    private final static Logger LOG = LoggerFactory.getLogger(AbstractDataTransaction.class);

    private final Object identifier;

    @Override
    public Object getIdentifier() {
        return this.identifier;
    }

    private TransactionStatus status;

    private final AbstractDataBroker<P, D, ? extends Object> broker;

    protected AbstractDataTransaction(final Object identifier,
            final AbstractDataBroker<P, D, ? extends Object> dataBroker) {
        super(dataBroker);
        this.identifier = identifier;
        this.broker = dataBroker;
        this.status = TransactionStatus.NEW;
        AbstractDataTransaction.LOG.debug("Transaction {} Allocated.", identifier);
    }

    @Override
    public Future<RpcResult<TransactionStatus>> commit() {
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
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AbstractDataTransaction other = (AbstractDataTransaction) obj;
        if (identifier == null) {
            if (other.identifier != null)
                return false;
        } else if (!identifier.equals(other.identifier))
            return false;
        return true;
    }

    @Override
    public TransactionStatus getStatus() {
        return this.status;
    }

    protected abstract void onStatusChange(final TransactionStatus status);

    public void changeStatus(final TransactionStatus status) {
        Object _identifier = this.getIdentifier();
        AbstractDataTransaction.LOG
                .debug("Transaction {} transitioned from {} to {}", _identifier, this.status, status);
        this.status = status;
        this.onStatusChange(status);
    }
}
