/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.spi.data;

public interface DOMStoreWriteTransaction extends DOMStoreTransaction, DOMStoreSyncMutator {
    /**
     *
     * Seals transaction, and returns three-phase commit cohort associated
     * with this transaction and DOM Store to be coordinated by coordinator.
     *
     * @return Three Phase Commit Cohort instance for this transaction.
     */
    DOMStoreThreePhaseCommitCohort ready();
}
