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
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractBackupConsumerService implements ClusterSingletonService {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractBackupConsumerService.class);
    private DOMDataBroker domDataBroker;

    public AbstractBackupConsumerService(final DOMDataBroker domDataBroker) {
        this.domDataBroker = domDataBroker;
    }

    protected abstract void initConsumer() throws IOException;

    protected abstract void startConsumption();

    protected abstract Boolean closeBackupConsumer();

    protected final void applyBackup(@NonNull final DataTreeCandidate candidate) {
        DOMDataTreeWriteTransaction writeTransaction = domDataBroker.newWriteOnlyTransaction();

        for (DataTreeCandidateNode node : candidate.getRootNode().getChildNodes()) {
            writeTransaction.put(LogicalDatastoreType.CONFIGURATION, YangInstanceIdentifier
                    .create(node.getIdentifier()), node.getDataAfter().get());
            LOG.info("Writing backup node: {}", node.toString());
        }

        try {
            LOG.debug("Commit writeTransaction");
            writeTransaction.commit().get();

        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error while commiting backup transaction");
        }
        writeTransaction.cancel();
    }

    @Override
    public void instantiateServiceInstance() {
        try {
            initConsumer();
        } catch (IOException e) {
            LOG.error("Backup Consumer initialization failed.", e);
            return;
        }
        startConsumption();
    }

    @Override
    public ListenableFuture<? extends Object> closeServiceInstance() {
        return Futures.immediateFuture(closeBackupConsumer());
    }
}
