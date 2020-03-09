/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.backup;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;

public abstract class AbstractBackupConsumerService implements ClusterSingletonService {

    protected abstract void startConsumption();

    protected abstract Boolean closeBackupConsumer();

    protected final void applyBackup(@NonNull final DataTreeCandidate candidate) {
        //TODO: apply candidate to dataStore

    }

    @Override
    public void instantiateServiceInstance() {
        startConsumption();
    }

    @Override
    public ListenableFuture<? extends Object> closeServiceInstance() {
        return Futures.immediateFuture(closeBackupConsumer());
    }
}
