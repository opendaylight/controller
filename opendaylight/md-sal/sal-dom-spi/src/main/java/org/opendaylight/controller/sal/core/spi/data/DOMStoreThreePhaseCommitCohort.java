/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.spi.data;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Interface implemented by the {@link DOMStore} and exposed for each {@link DOMStoreWriteTransaction}
 * upon its transition to Ready state. The frontend (DOMStore user) uses this interface to drive the
 * commit procedure across potentially multiple DOMStores using the Three-Phase-Commit (3PC) Protocol,
 * as described in {@link https://en.wikipedia.org/wiki/Three-phase_commit}.
 */
public interface DOMStoreThreePhaseCommitCohort {

    /**
     * Sends transaction associated with this three phase commit instance to the
     * participant, participant votes on the transaction, if the transaction
     * should be committed or aborted.
     *
     * @return ListenableFuture with vote of the participant. Vote
     *         {@link ListenableFuture#get()} is following:
     *         <ul>
     *         <li>
     *         true if transaction is approved by data store.
     *         <li>false if the transaction is not approved by data store and
     *         should be aborted.
     */
    ListenableFuture<Boolean> canCommit();

    /**
     * Initiates a pre-commit phase of associated transaction on datastore.
     *
     * This message is valid only and only if and only if the participant responded
     * on {@link #canCommit()} call with positive response.
     *
     * @return ListenableFuture representing acknowledgment for participant
     *        that pre-commit message was received and processed.
     */
    ListenableFuture<Void> preCommit();

    /**
     * Initiates a abort phase of associated transaction on data store.
     *
     * @return ListenableFuture representing acknowledgment for participant
     *        that abort message was received.
     */
    ListenableFuture<Void> abort();

    /**
     * Initiates a commit phase on of associated transaction on data store.
     *
     * @return ListenableFuture representing acknowledgment for participant
     *        that commit message was received and commit of transaction was
     *        processed.
     */
    ListenableFuture<Void> commit();
}
