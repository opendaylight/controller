/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

/**
 * This interface provides methods to persist data and is an abstraction of the akka-persistence persistence API.
 */
// FIXME: find a better name for this interface. It is heavily influenced by Pekko Persistence, most notably the weird
//        API around snapshots and message deletion -- which assumes the entity requesting it is the subclass itself.
public interface DataPersistenceProvider extends EntryStore, SnapshotStore {
    /**
     * Returns whether or not persistence recovery is applicable/enabled.
     *
     * @return {@code true} if recovery is applicable, otherwise false, in which case the provider is not persistent and
     *         may not have anything to be recovered
     */
    boolean isRecoveryApplicable();
}
