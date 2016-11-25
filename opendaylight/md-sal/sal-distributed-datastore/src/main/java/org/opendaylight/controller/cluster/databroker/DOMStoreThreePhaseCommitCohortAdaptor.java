/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker;

import com.google.common.base.Preconditions;
import com.google.common.collect.ForwardingObject;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;

/**
 * Utility class from bridging {@link DOMStoreThreePhaseCommitCohort} and
 * {@link org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort}.
 *
 * @author Robert Varga
 */
final class DOMStoreThreePhaseCommitCohortAdaptor extends ForwardingObject implements DOMStoreThreePhaseCommitCohort {
    private final org.opendaylight.mdsal.dom.spi.store.DOMStoreThreePhaseCommitCohort delegate;

    DOMStoreThreePhaseCommitCohortAdaptor(
        final org.opendaylight.mdsal.dom.spi.store.DOMStoreThreePhaseCommitCohort delegate) {
        this.delegate = Preconditions.checkNotNull(delegate);
    }

    @Override
    public ListenableFuture<Boolean> canCommit() {
        return delegate.canCommit();
    }

    @Override
    public ListenableFuture<Void> preCommit() {
        return delegate.preCommit();
    }

    @Override
    public ListenableFuture<Void> abort() {
        return delegate.abort();
    }

    @Override
    public ListenableFuture<Void> commit() {
        return delegate.commit();
    }

    @Override
    protected Object delegate() {
        return delegate;
    }
}
