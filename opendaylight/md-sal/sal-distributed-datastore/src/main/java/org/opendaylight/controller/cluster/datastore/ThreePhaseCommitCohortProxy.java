/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorPath;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;

import java.util.Collections;
import java.util.List;

/**
 * ThreePhaseCommitCohortProxy represents a set of remote cohort proxies
 */
public class ThreePhaseCommitCohortProxy implements
    DOMStoreThreePhaseCommitCohort{

    private final List<ActorPath> cohortPaths;

    public ThreePhaseCommitCohortProxy(List<ActorPath> cohortPaths) {

        this.cohortPaths = cohortPaths;
    }

    @Override public ListenableFuture<Boolean> canCommit() {
        throw new UnsupportedOperationException("canCommit");
    }

    @Override public ListenableFuture<Void> preCommit() {
        throw new UnsupportedOperationException("preCommit");
    }

    @Override public ListenableFuture<Void> abort() {
        throw new UnsupportedOperationException("abort");
    }

    @Override public ListenableFuture<Void> commit() {
        throw new UnsupportedOperationException("commit");
    }

    public List<ActorPath> getCohortPaths() {
        return Collections.unmodifiableList(this.cohortPaths);
    }
}
