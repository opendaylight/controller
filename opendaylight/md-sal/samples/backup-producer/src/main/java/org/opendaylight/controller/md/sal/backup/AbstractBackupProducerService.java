/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.backup;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collection;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.ClusteredDOMDataTreeChangeListener;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractBackupProducerService implements ClusterSingletonService,
        ClusteredDOMDataTreeChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractBackupProducerService.class);

    private final DOMDataBroker domDataBroker;
    private YangInstanceIdentifier watchedNode;

    public AbstractBackupProducerService(final DOMDataBroker domDataBroker) {
        this(domDataBroker, YangInstanceIdentifier.empty());
    }

    public AbstractBackupProducerService(final DOMDataBroker domDataBroker, final YangInstanceIdentifier watchedNode) {
        this.domDataBroker = requireNonNull(domDataBroker, "DOMDataBroker is missing");
        this.watchedNode = requireNonNull(watchedNode, "WatchedNode was null. Either provide" +
                " YangInstanceIdentifier or leave it blank and the default (root) will be used.");
    }

    protected abstract void sendToBackup(final DataTreeCandidate candidate);

    protected abstract Boolean closeBackupProducer();

    @Override
    public void instantiateServiceInstance() {
        LOG.info("Init AbstractBackupProducerService");
        domDataBroker.getExtensions().getInstance(DOMDataTreeChangeService.class).registerDataTreeChangeListener(
                new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, watchedNode), this);
    }

    @Override
    public void onDataTreeChanged(@NonNull Collection<DataTreeCandidate> changes) {
        for (DataTreeCandidate candidate : changes) {
            LOG.info("Received DataTreeCandidate change, sending to backup");
            sendToBackup(candidate);
        }
    }

    @Override
    public ListenableFuture<? extends Object> closeServiceInstance() {
        return Futures.immediateFuture(this.closeBackupProducer());
    }
}
