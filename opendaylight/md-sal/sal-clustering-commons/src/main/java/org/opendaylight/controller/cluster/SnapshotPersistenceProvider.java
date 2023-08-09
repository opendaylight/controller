/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster;

import akka.actor.ActorRef;
import akka.persistence.SnapshotOffer;
import akka.persistence.SnapshotProtocol;
import akka.persistence.SnapshotSelectionCriteria;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Optional;
import org.eclipse.jdt.annotation.NonNull;

public interface SnapshotPersistenceProvider {
    /**
     * Saves a snapshot.
     *
     * @param snapshot the snapshot object to save
     */
    void saveSnapshot(Object snapshot, String persistenceId, long snapshotSequenceNr, ActorRef actor);

    /**
     * Deletes snapshots based on the given criteria.
     *
     * @param criteria the search criteria
     */
    void deleteSnapshots(SnapshotSelectionCriteria criteria, String persistenceId, ActorRef actor);

    ListenableFuture<Optional<SnapshotOffer>> loadSnapshot(String persistenceId, SnapshotSelectionCriteria criteria);

    /**
     * Receive and potentially handle a {@link SnapshotProtocol} response.
     *
     * @param response A {@link SnapshotProtocol} response
     * @return {@code true} if the response was handled
     */
    boolean handleSnapshotResponse(SnapshotProtocol.@NonNull Response response);
}
