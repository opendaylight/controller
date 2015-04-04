/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.japi.Procedure;
import akka.persistence.SnapshotSelectionCriteria;
import java.util.concurrent.CountDownLatch;
import org.opendaylight.controller.cluster.DataPersistenceProvider;

/**
 * This class is intended for testing purposes. It just triggers CountDownLatch's in each method.
 * This class really should be under src/test/java but it was problematic trying to uses it in other projects.
 */
public class DataPersistenceProviderMonitor implements DataPersistenceProvider {

    private CountDownLatch persistLatch = new CountDownLatch(1);
    private CountDownLatch saveSnapshotLatch = new CountDownLatch(1);
    private CountDownLatch deleteSnapshotsLatch = new CountDownLatch(1);;
    private CountDownLatch deleteMessagesLatch = new CountDownLatch(1);;

    @Override
    public boolean isRecoveryApplicable() {
        return false;
    }

    @Override
    public <T> void persist(T o, Procedure<T> procedure) {
        persistLatch.countDown();
    }

    @Override
    public void saveSnapshot(Object o) {
        saveSnapshotLatch.countDown();
    }

    @Override
    public void deleteSnapshots(SnapshotSelectionCriteria criteria) {
        deleteSnapshotsLatch.countDown();
    }

    @Override
    public void deleteMessages(long sequenceNumber) {
        deleteMessagesLatch.countDown();
    }

    public void setPersistLatch(CountDownLatch persistLatch) {
        this.persistLatch = persistLatch;
    }

    public void setSaveSnapshotLatch(CountDownLatch saveSnapshotLatch) {
        this.saveSnapshotLatch = saveSnapshotLatch;
    }

    public void setDeleteSnapshotsLatch(CountDownLatch deleteSnapshotsLatch) {
        this.deleteSnapshotsLatch = deleteSnapshotsLatch;
    }

    public void setDeleteMessagesLatch(CountDownLatch deleteMessagesLatch) {
        this.deleteMessagesLatch = deleteMessagesLatch;
    }

    @Override
    public long getLastSequenceNumber() {
        return -1;
    }
}
