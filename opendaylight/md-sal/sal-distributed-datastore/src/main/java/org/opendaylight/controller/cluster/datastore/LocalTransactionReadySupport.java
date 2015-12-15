/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;

/**
 * Interface for a class that can "ready" a transaction.
 *
 * @author Thomas Pantelis
 */
interface LocalTransactionReadySupport {
    LocalThreePhaseCommitCohort onTransactionReady(@Nonnull DOMStoreWriteTransaction tx,
            @Nullable Exception operationError);
}
