/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster;

import akka.japi.Procedure;
import akka.persistence.SnapshotSelectionCriteria;

public interface DataPersistence {
    boolean isRecoveryApplicable();
    <T> void persist(T o, Procedure<T> procedure);
    void saveSnapshot(Object o);
    void deleteSnapshots(SnapshotSelectionCriteria criteria);
    void deleteMessages(long sequenceNumber);

}
