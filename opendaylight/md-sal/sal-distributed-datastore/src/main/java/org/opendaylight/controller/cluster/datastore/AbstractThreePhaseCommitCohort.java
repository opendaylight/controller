/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import scala.concurrent.Future;

/**
 * Abstract base class for {@link DOMStoreThreePhaseCommitCohort} instances returned by this
 * implementation. In addition to the usual set of methods it also contains the list of actor
 * futures.
 */
public abstract class AbstractThreePhaseCommitCohort<T> implements DOMStoreThreePhaseCommitCohort {
    protected static final ListenableFuture<Void> IMMEDIATE_VOID_SUCCESS = Futures.immediateFuture(null);
    protected static final ListenableFuture<Boolean> IMMEDIATE_BOOLEAN_SUCCESS = Futures.immediateFuture(Boolean.TRUE);

    abstract List<Future<T>> getCohortFutures();
}
