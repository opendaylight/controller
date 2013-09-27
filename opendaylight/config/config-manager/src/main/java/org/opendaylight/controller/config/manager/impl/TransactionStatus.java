/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl;

import javax.annotation.concurrent.GuardedBy;

public class TransactionStatus {
    @GuardedBy("this")
    private boolean secondPhaseCommitStarted = false;
    // switches to true during abort or commit failure
    @GuardedBy("this")
    private boolean aborted;
    // switches to true during second phase commit
    @GuardedBy("this")
    private boolean committed;

    public synchronized boolean isSecondPhaseCommitStarted() {
        return secondPhaseCommitStarted;
    }

    synchronized void setSecondPhaseCommitStarted() {
        this.secondPhaseCommitStarted = true;
    }

    public synchronized boolean isAborted() {
        return aborted;
    }

    synchronized void setAborted() {
        this.aborted = true;
    }

    public synchronized boolean isCommitted() {
        return committed;
    }

    synchronized void setCommitted() {
        this.committed = true;
    }

    public synchronized boolean isAbortedOrCommitted() {
        return aborted || committed;
    }

    public synchronized void checkNotCommitStarted() {
        if (secondPhaseCommitStarted == true)
            throw new IllegalStateException("Commit was triggered");
    }

    public synchronized void checkCommitStarted() {
        if (secondPhaseCommitStarted == false)
            throw new IllegalStateException("Commit was not triggered");
    }

    public synchronized void checkNotAborted() {
        if (aborted == true)
            throw new IllegalStateException("Configuration was aborted");
    }

    public synchronized void checkNotCommitted() {
        if (committed == true) {
            throw new IllegalStateException(
                    "Cannot use this method after second phase commit");
        }
    }

    public synchronized void checkCommitted() {
        if (committed == false) {
            throw new IllegalStateException(
                    "Cannot use this method before second phase commit");
        }
    }
}
