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
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.yang.common.Empty;
import scala.concurrent.Future;

/**
 * Abstract base class for {@link DOMStoreThreePhaseCommitCohort} instances returned by this
 * implementation. In addition to the usual set of methods it also contains the list of actor
 * futures.
 */
public abstract class AbstractThreePhaseCommitCohort<T> implements DOMStoreThreePhaseCommitCohort {
    protected static final @NonNull ListenableFuture<Empty> IMMEDIATE_EMPTY_SUCCESS =
        Futures.immediateFuture(Empty.value());
    protected static final @NonNull ListenableFuture<Boolean> IMMEDIATE_BOOLEAN_SUCCESS =
        Futures.immediateFuture(Boolean.TRUE);

    abstract List<Future<T>> getCohortFutures();
}
