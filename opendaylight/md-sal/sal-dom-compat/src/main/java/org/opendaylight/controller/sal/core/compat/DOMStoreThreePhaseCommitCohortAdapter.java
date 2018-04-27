/*
 * Copyright (c) 2018 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.compat;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ForwardingObject;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;

public class DOMStoreThreePhaseCommitCohortAdapter extends ForwardingObject implements DOMStoreThreePhaseCommitCohort {
    private final org.opendaylight.mdsal.dom.spi.store.DOMStoreThreePhaseCommitCohort delegate;

    public DOMStoreThreePhaseCommitCohortAdapter(
            final org.opendaylight.mdsal.dom.spi.store.DOMStoreThreePhaseCommitCohort delegate) {
        this.delegate = requireNonNull(delegate);
    }

    @Override
    protected org.opendaylight.mdsal.dom.spi.store.DOMStoreThreePhaseCommitCohort delegate() {
        return delegate;
    }

    @Override
    public ListenableFuture<Void> preCommit() {
        return delegate.preCommit();
    }

    @Override
    public ListenableFuture<Void> commit() {
        return delegate.commit();
    }

    @Override
    public ListenableFuture<Boolean> canCommit() {
        return delegate.canCommit();
    }

    @Override
    public ListenableFuture<Void> abort() {
        return delegate.abort();
    }
}
