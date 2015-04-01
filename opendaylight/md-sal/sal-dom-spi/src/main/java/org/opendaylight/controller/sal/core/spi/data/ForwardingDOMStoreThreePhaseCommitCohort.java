/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.spi.data;

import com.google.common.annotations.Beta;
import com.google.common.collect.ForwardingObject;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Abstract base class for {@link DOMStoreThreePhaseCommitCohort} implementations,
 * which forward most of their functionality to a backend {@link #delegate()}.
 */
@Beta
public abstract class ForwardingDOMStoreThreePhaseCommitCohort extends ForwardingObject implements DOMStoreThreePhaseCommitCohort {
    @Override
    protected abstract DOMStoreThreePhaseCommitCohort delegate();

    @Override
    public ListenableFuture<Boolean> canCommit() {
        return delegate().canCommit();
    }

    @Override
    public ListenableFuture<Void> preCommit() {
        return delegate().preCommit();
    }

    @Override
    public ListenableFuture<Void> abort() {
        return delegate().abort();
    }

    @Override
    public ListenableFuture<Void> commit() {
        return delegate().commit();
    }
}
