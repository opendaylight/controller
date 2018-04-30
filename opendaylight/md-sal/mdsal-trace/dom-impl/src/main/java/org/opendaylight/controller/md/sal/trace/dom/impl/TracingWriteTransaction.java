/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.trace.dom.impl;

import com.google.common.util.concurrent.FluentFuture;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.trace.closetracker.impl.CloseTracked;
import org.opendaylight.controller.md.sal.trace.closetracker.impl.CloseTrackedRegistry;
import org.opendaylight.controller.md.sal.trace.closetracker.impl.CloseTrackedTrait;
import org.opendaylight.mdsal.common.api.CommitInfo;

class TracingWriteTransaction extends AbstractTracingWriteTransaction
        implements CloseTracked<TracingWriteTransaction> {

    private final CloseTrackedTrait<TracingWriteTransaction> closeTracker;

    TracingWriteTransaction(DOMDataWriteTransaction delegate, TracingBroker tracingBroker,
            CloseTrackedRegistry<TracingWriteTransaction> writeTransactionsRegistry) {
        super(delegate, tracingBroker);
        this.closeTracker = new CloseTrackedTrait<>(writeTransactionsRegistry, this);
    }

    @Override
    public FluentFuture<? extends CommitInfo> commit() {
        closeTracker.removeFromTrackedRegistry();
        return super.commit();
    }

    @Override
    public boolean cancel() {
        closeTracker.removeFromTrackedRegistry();
        return super.cancel();
    }

    @Override
    public StackTraceElement[] getAllocationContextStackTrace() {
        return closeTracker.getAllocationContextStackTrace();
    }

    @Override
    public CloseTracked<TracingWriteTransaction> getRealCloseTracked() {
        return this;
    }

}
