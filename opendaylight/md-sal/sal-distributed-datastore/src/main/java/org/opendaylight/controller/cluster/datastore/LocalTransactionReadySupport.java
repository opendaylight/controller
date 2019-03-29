/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreWriteTransaction;

/**
 * Interface for a class that can "ready" a transaction.
 *
 * @author Thomas Pantelis
 */
interface LocalTransactionReadySupport {
    LocalThreePhaseCommitCohort onTransactionReady(@NonNull DOMStoreWriteTransaction tx,
            @Nullable Exception operationError);
}
